package ggack.agitpos.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ggack.agitpos.data.model.networkmodel.ItemModel
import ggack.agitpos.databinding.FragmentItemsBinding
import ggack.agitpos.listener.ListListener
import ggack.agitpos.ui.adapter.ItemAdapter
import ggack.agitpos.ui.dialog.AddItemDialog

@AndroidEntryPoint
class ItemListFragment : Fragment() {
    private var binding : FragmentItemsBinding? = null
    private var adapter : ItemAdapter? = null
    private val viewModel by activityViewModels<MainViewModel>()
    private var mListItems = mutableListOf<ItemModel>()
    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View? {
        binding = FragmentItemsBinding.inflate(inflater, container, false)
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
        initObserver()
        initAdapter()
        viewModel.refreshItems()
    }
    fun initObserver() {
        viewModel.liveItems.observe(viewLifecycleOwner) {
            it?.let {
                mListItems.clear()
                mListItems.addAll(it)
                mListItems.add(ItemModel("plusitem", 0, 0, 99999, -1))
                binding?.rv?.adapter?.notifyDataSetChanged()
            }
        }
    }

    fun initAdapter() {
        binding?.rv?.layoutManager = GridLayoutManager(requireContext(), 4)
        adapter = ItemAdapter(mListItems, object : ListListener {

            override fun onClick(key: String, item: Any?) {
                super.onClick(key, item)
                popUpItemChange(requireContext(),item as ItemModel, item.idx)
            }

            override fun onADD(key: String, item: Any?) {
                super.onADD(key, item)
                popUpItemChange(requireContext(), null, if(mListItems.size <= 1) 1 else (mListItems[mListItems.lastIndex - 1].idx + 1))
            }
        })
        binding?.rv?.adapter = adapter
        val itemTouchCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeFlag(
                    ItemTouchHelper.ACTION_STATE_DRAG,
                    ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.START or ItemTouchHelper.END
                )
            }
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from == mListItems.lastIndex)
                    return false
                adapter?.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                TODO("Not yet implemented")
            }

            override fun onMoved(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                from: Int,
                target: RecyclerView.ViewHolder,
                to: Int,
                x: Int,
                y: Int
            ) {
                if(from > to) {
                    var startIdx = if (to == 0) 1 else mListItems[to - 1].idx + 1
                    mListItems[from].idx = startIdx++
                    for (i in to until from) {
                        mListItems[i].idx = startIdx++
                    }
                } else {
                    var startIdx = if (to == 0) 1 else mListItems[from - 1].idx + 1
                    for (i in from + 1 .. to) {
                        mListItems[i].idx = startIdx++
                    }
                    mListItems[from].idx = startIdx++

                }
                Log.e("move", mListItems.toString())
                mListItems.sortBy { it.idx }
                super.onMoved(recyclerView, viewHolder, from, target, to, x, y)
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                viewModel.setItems(mListItems)
                super.clearView(recyclerView, viewHolder)
            }
        }

        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding?.rv)
    }

    private fun popUpItemChange(context : Context, item : ItemModel? = null, idx : Int) {
        val dlg = AddItemDialog(context, object : AddItemDialog.ResultCallback {
            override fun onResult(name: String, price: Int) {
                viewModel.addItem(ItemModel(name, price, 1, idx))
            }

            override fun onDelete() {
                if (item != null) {
                    viewModel.removeItem(item)
                }
            }

        })
        item?.let {
            dlg.setText(it.name, it.price)
            dlg.binding.btnDelete.visibility = View.VISIBLE
        }
        dlg.show()
    }
}