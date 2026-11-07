package ggack.agitpos.ui.main

import Singleton.isDuplicated
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout.LayoutParams
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.marginStart
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.UserModel
import ggack.agitpos.databinding.FragmentUserListBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.UserAdapter
import ggack.agitpos.ui.dialog.SimpleEditDialog
import ggack.agitpos.ui.dialog.SimpleListDialog
import ggack.agitpos.domain.Membership
import ggack.agitpos.domain.LedgerRequest
import ggack.agitpos.domain.LedgerAction
import java.util.UUID
import ggack.agitpos.utils.Utils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class UserListFragment : Fragment() {
    @Inject
    lateinit var firebaseComm: FirebaseComm
    private var binding : FragmentUserListBinding? = null
    private var adapter : UserAdapter? = null
    private val viewModel by activityViewModels<MainViewModel>()
    var mDlgHistory : SimpleListDialog? = null
    var currentSelected = ""
    var mapUserList = mapOf<String, Map<String, Any>>()
    var mapHistoryByName = mutableMapOf<String, MutableList<String>>()
    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.btnAddUser?.setOnClickListener {
            popupAddUser(requireContext())
        }
        initObserver()
        initAdapter()
        viewModel.refreshUser()
    }

    fun initObserver() {
        viewModel.liveUserData.observe(viewLifecycleOwner) {
            it?.let {
                mapUserList = it
                adapter?.map = it
                adapter?.keys = it.keys.toList().sorted()
                adapter?.notifyDataSetChanged()
            }
        }
        viewModel.liveHistoryByName.observe(viewLifecycleOwner) {
            it?.keys?.forEach { key ->
                it[key]?.let {list ->
                    val newList = mutableListOf<String>()
                    list.sortBy { it.date }
                    list.forEach { item ->
                        val date = Utils.getDate(item.date).format(DateTimeFormatter.ofPattern("yy.MM.dd일 HH:mm:ss"))
                        var text = "$date : [${item.kind}]"
                        if(item.money != 0)
                            text += " [금액 : ${item.money}]"
                        if(item.item?.keys != null && item.item?.keys?.isNotEmpty() == true)
                            text += " ${item.item?.keys}"
                        if(item.etc.isNotEmpty())
                            text += " [기타 : ${item.etc}]"
                        newList.add(text)
                    }
                    mapHistoryByName[key] = newList
                }
                if(mDlgHistory?.isShowing() == true && currentSelected != "") mapHistoryByName[currentSelected]?.let { it1 ->
                    mDlgHistory?.refreshList(
                        it1
                    )
                }
            }
        }
    }

    fun initAdapter() {
        binding?.recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserAdapter(mapUserList, object : ListListener {
            override fun onRemove(key: String, item: Any?) {
                super.onRemove(key, item)
                currentSelected = key
                AlertDialog.Builder(requireContext())
                    .setMessage("[$key] 를 삭제합니까?")
                    .setPositiveButton("네") { _, _ ->
                        viewModel.removeUser(key)
                    }
                    .setNegativeButton("아니오") { _, _ ->
                    }.create().show()
            }

            override fun onChange(key: String, item: Any?) {
                super.onChange(key, item)
                currentSelected = key
                popUpChangeMoney(requireContext(),key, (item as UserModel?)?.money?:0)
            }

            override fun onRecord(key: String, item: Any?) {
                super.onRecord(key, item)
                currentSelected = key
                mDlgHistory =
                    if(mapHistoryByName[key] != null) SimpleListDialog(requireContext(), mapHistoryByName[key]!!) {} else
                        SimpleListDialog(requireContext(), listOf()) {}
                mDlgHistory?.setListMargin(0)
                mDlgHistory?.setListTextSize(10)
                mDlgHistory?.setOnRefreshListener {
                    firebaseComm.getPrevHistory()
                    mDlgHistory?.setRefresh(false)
                }
                mDlgHistory?.show()
            }

            override fun onMember(key: String, item: Any?) {
                super.onMember(key, item)
                item as UserModel
                currentSelected = key

                val now = LocalDateTime.now()
                val active = Membership.expiry(item.member)?.isAfter(now) == true
                firebaseComm.setMembership(key, if (active) "" else Membership.encode(Membership.monthly(now)))

            }

            override fun onADD(key: String, item: Any?) {
                super.onADD(key, item)
                item as UserModel
                val dlg = SimpleEditDialog(requireContext(), SimpleEditDialog.TYPE_ONE_BUTTON) { result, data ->
                    if(data.isNotEmpty()) {
                        if(data.isNotEmpty()) {
                            if (mapUserList.isDuplicated(key, data))
                                Toast.makeText(requireContext(), "이름이 중복됩니다", Toast.LENGTH_SHORT)
                                    .show()
                            else
                                firebaseComm.setExtraName(key, data)
                        }
                    }
                }
                dlg.setText("별도로 설정할 입금자명을 입력해주세요")
                if(!item.extraName.isNullOrEmpty())
                    dlg.setEditText(item.extraName?:"")
                dlg.show()
            }

            override fun onSupervisor(key: String, item: Any?) {
                super.onSupervisor(key, item)
                item as UserModel
                currentSelected = key
                firebaseComm.setSupervisor(key, item.isSupervisor != true )
            }
        })
        binding?.recyclerView?.adapter = adapter
    }


    private fun popUpChangeMoney(context : Context, name : String, money : Int) {
        val editText = EditText(context)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.setText(money.toString())
        editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val dlg = android.app.AlertDialog.Builder(context).setView(editText).setTitle("금액 수정").setPositiveButton("OK"
        ) { _, _ ->
            try {
                val v = editText.text.toString().toInt()
                firebaseComm.submit(LedgerRequest(UUID.randomUUID().toString(), System.currentTimeMillis(),
                    LedgerAction.SetBalance(name, money, v))) { result ->
                    if (binding != null) Toast.makeText(context, result.fold(
                        onSuccess = { "입력 성공" }, onFailure = { it.message ?: "입력 실패" }
                    ), Toast.LENGTH_LONG).show()
                    viewModel.refreshUser()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "입력 실패", Toast.LENGTH_SHORT).show()
            }
        }
        dlg.show()
    }

    private fun popupAddUser(context : Context) {
        val editText = EditText(context)
        editText.hint = "닉네임을 입력해 주세요"
        val dlg = android.app.AlertDialog.Builder(context).setView(editText).setTitle("유저 추가").setPositiveButton("OK"
        ) { _, _ ->
            if(editText.text.toString().isNotEmpty()) {
                if (viewModel.liveUserData.value?.isDuplicated(
                        editText.text.toString(),
                        editText.text.toString()
                    ) == true
                )
                    Toast.makeText(context, "이름이 중복됩니다", Toast.LENGTH_SHORT).show()
                else
                    viewModel.addUser(editText.text.toString())
            }
        }.setNegativeButton("Cancel", null)
        dlg.show()
    }
}