package ggack.agitpos.ui.main

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.data.model.networkmodel.UserModel
import ggack.agitpos.databinding.FragmentMainMasterChargeBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.ItemAdapter
import ggack.agitpos.ui.adapter.PaymentAdapter
import ggack.agitpos.ui.adapter.UserNameAdapter
import ggack.agitpos.ui.dialog.AddItemDialog
import ggack.agitpos.utils.Constants
import ggack.agitpos.ui.dialog.SimpleDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class MasterChargeFragment : Fragment() {
    @Inject lateinit var firebaseComm: FirebaseComm
    private var binding : FragmentMainMasterChargeBinding? = null
    private val viewModel by activityViewModels<MainViewModel>()
    private var mListPayment = mutableListOf<ItemModel>()
    private var mTime = LocalTime.of(0,1,0)
    private var mTotal = 0
    private var mUserData = mapOf<String, Map<String, Any>>()
    private var mUserName = ""
    private var mUser : UserModel? = null
    private var mUserNameAdapter : UserNameAdapter? = null
    private var mListItems =
        mutableListOf(
            ItemModel("일일 이용권", 6000, 1),
            ItemModel("'월간 이용권", 40000, 1),
            ItemModel("캔음료", 1300, 1),
            ItemModel("라면", 2000, 1),
            ItemModel("고급라면", 2500, 1),
            ItemModel("계란", 500, 1),
            ItemModel("아메리카노", 1000, 1),
            ItemModel("에이드류", 1000, 1),
            ItemModel("라떼류", 2000, 1),
            ItemModel("밀크티", 2000, 1)
        )

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentMainMasterChargeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        binding?.tvName?.setOnClickListener {
//            (activity as MainActivity).startFragment(UserListFragment())
//        }
        initItemAdapter()
        initObserver()
        initPaymentAdapter()
        initButtons()
        initViews()
        initAdapter()
    }
    fun initButtons() {
        binding?.cvAdd?.setOnClickListener {
        }
        binding?.btnPayment?.setOnClickListener {
            mUser?.let { user ->
                    if(mUserName.isNotEmpty()) {
                    val dlg = SimpleDialog(requireContext(), SimpleDialog.TYPE_TWO_BUTTON) {
                        when (it) {
                            RESULT_OK -> {
                                val map = mutableMapOf<String, Map<String, Any>>()
                                mListPayment.forEach { model ->
                                    map[model.name] = model.getMap()
                                }
                                if (map.keys.contains("월간 이용권"))
                                    firebaseComm.setMembership(
                                        mUserName,
                                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                                    )
                                firebaseComm.setMoney(mUserName, user.money - mTotal)
                                firebaseComm.addHistory(
                                    HistoryModel(
                                        Instant.now().toEpochMilli(),
                                        mUserName,
                                        Constants.HISTORY_PAYMENT,
                                        mTotal,
                                        map,
                                        "마스터"
                                    )
                                )
                                Toast.makeText(requireContext(), "결제가 완료되었습니다.", Toast.LENGTH_SHORT)
                                    .show()
                                activity?.onBackPressed()
                            }
                            RESULT_CANCELED -> {

                            }
                        }
                    }
                    dlg.setText("${binding?.tvTotal?.text} 을 결제하시겠습니까?")
                    dlg.show()
                }
            }
        }

    }
    fun initViews() {
        binding?.etNickname?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {
                val list = viewModel.GetUserByConsonant(s.toString())
                mUserNameAdapter?.keys = list.sorted()
                mUserNameAdapter?.notifyDataSetChanged()
                setCurrentUser(s.toString())
            }
        })
        binding?.root?.setOnClickListener {
            binding?.rvNickName?.visibility = View.GONE
        }
        binding?.etNickname?.setOnClickListener {
            val imm = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding?.etNickname,0)
            binding?.etNickname?.setText("")
            binding?.rvNickName?.visibility = View.VISIBLE
        }
        binding?.etNickname?.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus) {
                binding?.etNickname?.setText("")
                binding?.rvNickName?.visibility = View.VISIBLE
            } else {
                binding?.rvNickName?.visibility = View.GONE
            }
        }
        binding?.cvAdd?.setOnClickListener {
            val dlg = AddItemDialog(requireContext(), object: AddItemDialog.ResultCallback{
                override fun onResult(name: String, price: Int) {
                    if(!name.isNullOrEmpty()) {
                        mListItems.add(ItemModel(name, price, 1))
                        binding?.rvItems?.adapter?.notifyItemInserted(mListItems.lastIndex)
                    }
                }

                override fun onDelete() {
                    TODO("Not yet implemented")
                }

            })
            dlg.setText("추가할 항목을 입력해주세요")
            dlg.show()
        }
    }

    fun initObserver() {
        viewModel.liveUserData.observe(viewLifecycleOwner) {
            it?.let {
                mUserData = it
                mUserNameAdapter?.map = it
                mUserNameAdapter?.keys = it.keys.toList().sorted()
                mUserNameAdapter?.notifyDataSetChanged()
                setCurrentUser(mUserName)
            }
        }
        viewModel.liveItems.observe(viewLifecycleOwner) {
            it?.let {
                mListItems.clear()
                mListItems.addAll(it)
                binding?.rvItems?.adapter?.notifyDataSetChanged()
            }
        }
    }
    fun setCurrentUser(name : String) {
        if (name != "" && mUserData.contains(name) && !mUserData[name].isNullOrEmpty()) {
            mUser = FromMap.toUserModel(mUserData[name]!!)
            mUserName = name
            binding?.tvPoint?.text = "${mUser?.money} pt"
        }
    }
    private fun initAdapter() {
        binding?.rvNickName?.layoutManager = LinearLayoutManager(requireContext())
        mUserNameAdapter = UserNameAdapter(hashMapOf(), object : ListListener{
            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                binding?.etNickname?.setText(key)
                binding?.rvNickName?.visibility = View.GONE
            }
        })

        binding?.rvNickName?.adapter = mUserNameAdapter
    }

    fun initItemAdapter() {
        binding?.rvItems?.layoutManager = GridLayoutManager(requireContext(), 4)
        binding?.rvItems?.adapter = ItemAdapter(mListItems, object : ListListener {
            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                val position = mListPayment.has(key)
                if(position >= 0) {
                    mListPayment[position].cnt++
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
        binding?.tvTotal?.text = "$mTotal pt"
    }
}