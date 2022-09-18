package ggack.agitpos.ui.view.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ggack.agitpos.data.model.ItemModel
import ggack.agitpos.databinding.FragmentMainClientBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.ItemAdapter
import ggack.agitpos.ui.adapter.PaymentAdapter
import ggack.agitpos.ui.view.activity.MainActivity
import ggack.agitpos.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
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
        binding?.tvName?.setOnClickListener {
            (activity as MainActivity).startFragment(UserListFragment())
        }
        initObserver()
        initItemAdapter()
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
    }

    fun initObserver() {
        viewModel.liveCurrentUser.observe(viewLifecycleOwner) {user ->
            user?.let {
                binding?.tvName?.text = viewModel.userName
                binding?.tvPoint?.text = "${user.money} pt +"
            }
        }
    }
    fun initItemAdapter() {
        val listItems = listOf(
            ItemModel("일일 이용권", 6000, 1, null),
            ItemModel("월간 이용권", 40000, 1, null),
            ItemModel("캔음료", 1000, 1, null),
            ItemModel("라면", 2000, 1, null),
            ItemModel("계란", 500, 1, null),
            ItemModel("제조음료", 1000, 1, null),
            ItemModel("우유음료", 2000, 1, null)
        )
        binding?.rvItems?.layoutManager = GridLayoutManager(requireContext(), 4)
        binding?.rvItems?.adapter = ItemAdapter(listItems, object : ListListener {
            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                val position = mListPayment.has(key)
                if(position >= 0) {
                    mListPayment[position].cnt++
                    binding?.rvSelectedItems?.adapter?.notifyItemChanged(position)
                } else {
                    mListPayment.add(item as ItemModel)
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
        var total = 0
        mListPayment.forEach {
            total += it.price * it.cnt
        }
        binding?.tvTotal?.text = "$total pt"
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