package ggack.agitpos.ui.main

import Singleton.isDuplicated
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.data.model.networkmodel.UserModel
import ggack.agitpos.databinding.FragmentMainClientBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.ItemAdapter
import ggack.agitpos.ui.adapter.PaymentAdapter
import ggack.agitpos.ui.dialog.SimpleDialog
import ggack.agitpos.ui.dialog.SimpleEditDialog
import ggack.agitpos.ui.dialog.SimpleListDialog
import ggack.agitpos.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.timer

@AndroidEntryPoint
class MainClientFragment : Fragment() {
    @Inject lateinit var firebaseComm: FirebaseComm
    private var binding : FragmentMainClientBinding? = null
    private val viewModel by activityViewModels<MainViewModel>()
    private var mListPayment = mutableListOf<ItemModel>()
    private var mLogoutTimer : Timer? = null
    private var mTime = LocalTime.of(0,1,0)
    private var mUser : UserModel? = null
    private var mTotal = 0
    private val mListItems = mutableListOf<ItemModel>()
    private var mListHistory = mutableListOf<String>()
    private var mDlgHistory : SimpleListDialog? = null

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentMainClientBinding.inflate(inflater, container, false)
        binding?.vm = viewModel
        binding?.lifecycleOwner = viewLifecycleOwner
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initItemAdapter()
        initObserver()
        initPaymentAdapter()
        initButtons()
        initViews()
    }
    fun initViews() {
        binding?.tvMember?.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            activity?.runOnUiThread {
                binding?.tvMember?.visibility = View.GONE
            }
        }

    }
    fun initButtons() {
        binding?.cvTimeOut?.setOnClickListener {
            activity?.onBackPressed()
        }
        binding?.cvPinChange?.setOnClickListener {
            popUpChangePin(requireContext())
        }
        binding?.cvNickNameChange?.setOnClickListener {
            val dlg = SimpleEditDialog(requireContext(), SimpleEditDialog.TYPE_ONE_BUTTON) { result, data ->
                if(data.isNotEmpty()) {
                    if(data.isNotEmpty()) {
                        if (viewModel.liveUserData.value?.isDuplicated(viewModel.liveCurrentUserName.value?:"", data) == true)
                            Toast.makeText(requireContext(), "이름이 중복됩니다", Toast.LENGTH_SHORT)
                                .show()
                        else
                            firebaseComm.setExtraName(viewModel.liveCurrentUserName.value?:"", data)
                    }
                }
            }
            dlg.setText("별도로 설정할 입금자명을 입력해주세요")
            if(!viewModel.liveCurrentUserExtraName.value.isNullOrEmpty())
                dlg.setEditText(viewModel.liveCurrentUserExtraName.value?:"")
            dlg.show()
        }
        binding?.cvHistory?.setOnClickListener {
            mDlgHistory = SimpleListDialog(requireContext(), mListHistory) {}
            mDlgHistory?.setListMargin(0)
            mDlgHistory?.setListTextSize(10)
            mDlgHistory?.setOnRefreshListener {
                firebaseComm.getPrevHistory()
                mDlgHistory?.setRefresh(false)
            }
            mDlgHistory?.show()
        }
        binding?.btnPayment?.setOnClickListener {
            val dlg = SimpleDialog(requireContext(), SimpleDialog.TYPE_TWO_BUTTON) {
                when (it) {
                    RESULT_OK -> {
                        viewModel.onPayment(mListPayment) { result ->
                            if (binding == null) return@onPayment
                            context?.let { Toast.makeText(it, result.fold(
                                onSuccess = { "결제가 완료되었습니다." },
                                onFailure = { it.message ?: "결제를 저장하지 못했습니다." }
                            ), Toast.LENGTH_LONG).show() }
                            if (result.isSuccess) activity?.onBackPressed()
                        }
                    }
                    RESULT_CANCELED -> {

                    }
                }
            }
            dlg.setText("${binding?.tvTotal?.text} 을 결제하시겠습니까?")
            dlg.show()
        }

    }

    fun initObserver() {
        viewModel.liveSaving.observe(viewLifecycleOwner) { saving ->
            binding?.btnPayment?.isEnabled = !saving
            binding?.btnPayment?.text = if (saving) "저장 중…" else "결제"
        }
        viewModel.liveCurrentUserMember.observe(viewLifecycleOwner) {
            if (it) {
                for (i in mListItems.lastIndex downTo 0) {
                    if (mListItems[i].name.contains("회원권")) {
                        mListItems.removeAt(i)
                        binding?.rvItems?.adapter?.notifyItemRemoved(i)
                    }
                }
                binding?.tvMember?.text = "회원권 적용중입니다. \n${viewModel.liveCurrentUserMemberDate.value}"
            } else {
                binding?.tvMember?.text = "이용권 결제가 필요합니다."
            }
        }
        viewModel.liveCurrentUserMemberDate.observe(viewLifecycleOwner) {
            it?.let {
                if(viewModel.liveCurrentUserMember.value == true) {
                    binding?.tvMember?.text = "회원권 적용중입니다. \n$it"
                }
            }
        }
        viewModel.liveItems.observe(viewLifecycleOwner) {
            it?.let {
                mListItems.clear()
                mListItems.addAll(it)
                var n = -1
                for((i, item) in mListItems.withIndex()) {
                    if (item.name == "월간 회원권")
                        n = i
                }
                if(n >= 0 && LocalDateTime.now().dayOfMonth > 20)
                    mListItems.removeAt(n)
                if (viewModel.liveCurrentUserMember.value == true) {
                    for (i in mListItems.lastIndex downTo 0) {
                        if (mListItems[i].name.contains("회원권")) {
                            mListItems.removeAt(i)
                        }
                    }
                }
                binding?.rvItems?.adapter?.notifyDataSetChanged()
                if(viewModel.liveCurrentUserSupervisor.value == true) {
                    var bonus = viewModel.liveCurrentUserBonus.value?:2022120100L
                    var cnt = bonus % 100L
                    val dtf = DateTimeFormatter.ofPattern("yyyyMMdd")
                    val date = LocalDate.parse(Math.floorDiv(bonus, 100).toString(), dtf)
                    val today = LocalDate.now()

                    //같은날이면 x
                    if(date.isBefore(today)) {
                        //새로운달이면 처음부터
                        //8번 이상이면 x
                        if (date.monthValue != today.monthValue)
                            cnt = 0L
                    }
                    cnt++

                    mListItems.add(
                        ItemModel(
                            "운영진 출첵 $cnt/8",
                            0,
                            1
                        )
                    )
                }
                binding?.rvItems?.adapter?.notifyDataSetChanged()
            }
        }
        viewModel.liveCurrentUserBonus.observe(viewLifecycleOwner) {

        }
        viewModel.liveCurrentUserSupervisor.observe(viewLifecycleOwner) {

        }
        viewModel.liveHistoryByName.observe(viewLifecycleOwner) {
            CoroutineScope(Dispatchers.Main).launch {
                var n = 0
                while((viewModel.liveCurrentUserName.value ?: "") == "") {
                    delay(100)
                    if(n++ > 20)
                        return@launch
                }
                it?.get(viewModel.liveCurrentUserName.value?:"")?.let { list ->
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
                    mListHistory = newList
                }
                Handler(Looper.getMainLooper()).post {
                    if(mDlgHistory?.isShowing() == true)
                        mDlgHistory?.refreshList(mListHistory)
                }

            }
        }
    }
    fun initItemAdapter() {
        binding?.rvItems?.layoutManager = GridLayoutManager(requireContext(), 4)
        binding?.rvItems?.adapter = ItemAdapter(mListItems, object : ListListener {
            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                val position = mListPayment.has(key)
                if(position >= 0) {
                    mListPayment[position].cnt++
                    if(mListPayment[position].name.contains("운영진 출첵"))
                        mListPayment[position].cnt = 1
                    binding?.rvSelectedItems?.adapter?.notifyItemChanged(position)
                } else {
                    mListPayment.add((item as ItemModel).copy())
                    binding?.rvSelectedItems?.adapter?.notifyItemInserted(mListPayment.lastIndex)
                }
                setTotal()
            }
        })
    }
    fun initPaymentAdapter() {
        binding?.rvSelectedItems?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rvSelectedItems?.adapter = PaymentAdapter(mListPayment, object : ListListener {
            override fun onChange(position: Int) {
                super.onChange(position)
                setTotal()
                Log.e("rvSelectedItems", mListPayment.toString())
            }
        })
    }
    fun MutableList<ItemModel>.has(name : String) : Int {
        for((i, model) in this.withIndex()) {
            if(model.name == name)
                return i
        }
        return -1
    }
    fun setTotal() {
        mTotal = 0
        mListPayment.forEach {
            mTotal += it.price * it.cnt
        }
        binding?.tvTotal?.text = "￦ $mTotal"
    }
    fun setTimer() {
        mLogoutTimer?.cancel()
        mLogoutTimer = timer(initialDelay = 1000, period = 1000) {
            if (viewModel.liveSaving.value != true) mTime = mTime.minusSeconds(1)
            Handler(Looper.getMainLooper()).post{
                val sdf = DateTimeFormatter.ofPattern("m:ss")
                binding?.tvLogOutTime?.text = mTime.format(sdf)
                if(mTime.second == 0 && viewModel.liveSaving.value != true) {
                    Toast.makeText(context, "로그인 시간이 만료되었습니다.", Toast.LENGTH_SHORT).show()
                    activity?.onBackPressed()
                }
            }
        }
    }

    override fun onResume() {
        setTimer()
        super.onResume()
    }

    override fun onPause() {
        mLogoutTimer?.cancel()
        super.onPause()
    }


    private fun popUpChangePin(context : Context, name : String = viewModel.liveCurrentUserName.value?:"") {
        val editText = EditText(context)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        editText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(4))
        val dlg = AlertDialog.Builder(context).setView(editText).setTitle("Pin 수정").setPositiveButton("OK"
        ) { _, _ ->
            if(editText.text.length != 4) {
                Toast.makeText(context, "짧습니다", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    firebaseComm.changePin(name, editText.text.toString())
                    Toast.makeText(context, "수정 성공", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dlg.show()
    }
}