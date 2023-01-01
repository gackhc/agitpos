package ggack.agitpos.ui.main

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
import ggack.agitpos.R
import ggack.agitpos.data.firebase.FirebaseComm
import ggack.agitpos.data.model.networkmodel.FromMap
import ggack.agitpos.data.model.networkmodel.HistoryModel
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.data.model.networkmodel.UserModel
import ggack.agitpos.databinding.FragmentMainClientBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.ItemAdapter
import ggack.agitpos.ui.adapter.PaymentAdapter
import ggack.agitpos.utils.Constants
import ggack.agitpos.ui.dialog.SimpleDialog
import ggack.agitpos.ui.dialog.SimpleListDialog
import ggack.agitpos.utils.Utils
import java.time.Instant
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
    }
    fun initButtons() {
        binding?.cvTimeOut?.setOnClickListener {
            activity?.onBackPressed()
        }
        binding?.cvPinChange?.setOnClickListener {
            popUpChangePin(requireContext())
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
                        viewModel.onPayment(mListPayment)
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

    fun initObserver() {
        viewModel.liveCurrentUserMonthly.observe(viewLifecycleOwner) {
            if(it) {
                var n = -1
                for((i, item) in mListItems.withIndex()) {
                    if (item.name == "월간 회원권")
                        n = i
                }
                if(n >= 0)
                    mListItems.removeAt(n)
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
                if(viewModel.liveCurrentUserSupervisor.value == true)
                    mListItems.add(ItemModel("운영진 출첵", 0, 1))
                binding?.rvItems?.adapter?.notifyDataSetChanged()
            }
        }
        viewModel.liveCurrentUserBonus.observe(viewLifecycleOwner) {

        }
        viewModel.liveCurrentUserSupervisor.observe(viewLifecycleOwner) {

        }
        viewModel.liveHistoryByName.observe(viewLifecycleOwner) {
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
            if(mDlgHistory?.isShowing() == true)
                mDlgHistory?.refreshList(mListHistory)
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
                    if(mListPayment[position].name == "운영진 출첵")
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