package net.codinux.banking.fints4k.android

import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import net.codinux.banking.fints4k.android.adapter.AccountTransactionsListRecyclerAdapter
import net.codinux.banking.fints4k.android.databinding.FragmentFirstBinding
import net.codinux.banking.fints4k.android.dialogs.EnterTanDialog

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    private val accountTransactionsAdapter = AccountTransactionsListRecyclerAdapter()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rcyvwAccountTransactions.apply {
            layoutManager = LinearLayoutManager(this@FirstFragment.context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(ContextThemeWrapper(this@FirstFragment.context, R.style.Theme_Fints4kProject), (layoutManager as LinearLayoutManager).orientation))
            adapter = accountTransactionsAdapter
        }

        val presenter = Presenter() // TODO: inject

        presenter.enterTanCallback = { tanChallenge ->
            EnterTanDialog().show(tanChallenge, activity!!)
        }

        // TODO: set your credentials here
        presenter.retrieveAccountData("", "", "", "") { response ->
            response.customerAccount?.let { customer ->
                accountTransactionsAdapter.items = customer.accounts.flatMap { it.bookedTransactions }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}