package ggack.agitpos.ui.view.fragment

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.HistoryModel
import ggack.agitpos.data.model.ItemModel
import ggack.agitpos.data.model.UserModel
import ggack.agitpos.databinding.FragmentMainClientBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.ItemAdapter
import ggack.agitpos.ui.adapter.PaymentAdapter
import ggack.agitpos.ui.view.activity.MainActivity
import ggack.agitpos.ui.viewmodel.MainViewModel
import ggack.agitpos.utils.Constants
import ggack.agitpos.utils.SimpleDialog
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timer

class MainClientFragment : Fragment() {
    private var binding : FragmentMainClientBinding? = null
    private lateinit var viewModel : MainViewModel
    private var mListPayment = mutableListOf<ItemModel>()
    private var mLogoutTimer : Timer? = null
    private var mTime = LocalTime.of(0,1,0)
    private var mUser : UserModel? = null
    private var mTotal = 0
    private val mListItems = if(LocalDateTime.now().dayOfMonth < 16) {
        mutableListOf(
            ItemModel("일일 이용권", 6000, 1, null),
            ItemModel("월간 이용권", 40000, 1, null),
            ItemModel("캔음료", 1300, 1, null),
            ItemModel("라면", 2000, 1, null),
            ItemModel("고급라면", 2500, 1, null),
            ItemModel("계란", 500, 1, null),
            ItemModel("아메리카노", 1000, 1, null),
            ItemModel("에이드류", 1000, 1, null),
            ItemModel("라떼류", 2000, 1, null),
            ItemModel("밀크티", 2000, 1, null)
        )
    } else {
        mutableListOf(
            ItemModel("일일 이용권", 6000, 1, null),
            ItemModel("캔음료", 1300, 1, null),
            ItemModel("라면", 2000, 1, null),
            ItemModel("고급라면", 2500, 1, null),
            ItemModel("계란", 500, 1, null),
            ItemModel("아메리카노", 1000, 1, null),
            ItemModel("에이드류", 1000, 1, null),
            ItemModel("라떼류", 2000, 1, null),
            ItemModel("밀크티", 2000, 1, null))
    }
    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentMainClientBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
//        binding?.tvName?.setOnClickListener {
//            (activity as MainActivity).startFragment(UserListFragment())
//        }
        initItemAdapter()
        initObserver()
        initPaymentAdapter()
        initButtons()
    }
    fun initButtons() {
        binding?.cvTimeOut?.setOnClickListener {
            activity?.onBackPressed()
        }
        binding?.cvPinChange?.setOnClickListener {
            viewModel.popUpChangePin(requireContext())
        }
        binding?.btnPayment?.setOnClickListener {
            mUser?.let { user ->
                val dlg = SimpleDialog(requireContext(), SimpleDialog.TYPE_TWO_BUTTON) {
                    when (it) {
                        RESULT_OK -> {
                            val map = mutableMapOf<String, Map<String, Any>>()
                            mListPayment.forEach {model->
                                map[model.name] = model.getMap()
                            }
                            if(map.keys.contains("월간 이용권"))
                                FirebaseComm.setMembership(viewModel.userName,
                                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")))
                            FirebaseComm.setMoney(viewModel.userName, user.money - mTotal)
                            FirebaseComm.addHistory(HistoryModel(
                                Instant.now().toEpochMilli(),
                                viewModel.userName,
                                Constants.HISTORY_PAYMENT,
                                mTotal,
                                map,
                                ""
                            ))
                            Toast.makeText(requireContext(), "결제가 완료되었습니다.", Toast.LENGTH_SHORT).show()
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

    fun initObserver() {
        viewModel.liveCurrentUser.observe(viewLifecycleOwner) {user ->
            user?.let {
                mUser = it
                if(mUser?.member == LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))){
                    binding?.tvName?.text = "${viewModel.userName} (월회원)"
                    binding?.tvName?.setTextColor(requireContext().getColor(R.color.red))
                    var n = -1
                    for((i, item) in mListItems.withIndex()) {
                        if (item.name == "월간 회원권")
                            n = i
                    }
                    if(n >= 0)
                        mListItems.removeAt(n)
                } else {
                    binding?.tvName?.text = viewModel.userName
                    binding?.tvName?.setTextColor(requireContext().getColor(R.color.black))
                }
                binding?.tvPoint?.text = "${user.money} pt"
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
    fun setTimer() {
        mLogoutTimer?.cancel()
        mLogoutTimer = timer(initialDelay = 1000, period = 1000) {
            mTime = mTime.minusSeconds(1)
            Handler(Looper.getMainLooper()).post{
                val sdf = DateTimeFormatter.ofPattern("m:ss")
                binding?.tvLogOutTime?.text = mTime.format(sdf)
                if(mTime.second == 0) {
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

}