package net.dankito.banking.fints4java.android.ui.home

import android.app.SearchManager
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import net.dankito.banking.fints4java.android.MainActivity
import net.dankito.banking.fints4java.android.R
import net.dankito.banking.fints4java.android.ui.adapter.AccountTransactionAdapter
import net.dankito.banking.ui.model.AccountTransaction
import net.dankito.banking.ui.model.parameters.TransferMoneyData
import net.dankito.banking.ui.model.responses.GetTransactionsResponse
import net.dankito.banking.ui.presenter.MainWindowPresenter
import net.dankito.utils.android.extensions.asActivity


class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var mnitmBalance: MenuItem

    private lateinit var mnitmSearchTransactions: MenuItem

    private lateinit var mnitmUpdateTransactions: MenuItem


    private val transactionAdapter = AccountTransactionAdapter()

    protected var appliedTransactionsFilter = ""


    private lateinit var presenter: MainWindowPresenter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
//        val textView: TextView = root.findViewById(R.id.text_home)
//        homeViewModel.text.observe(this, Observer {
//            textView.text = it
//        })


        val rcyvwAccountTransactions: RecyclerView = root.findViewById(R.id.rcyvwAccountTransactions)
        rcyvwAccountTransactions.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rcyvwAccountTransactions.adapter = transactionAdapter

        registerForContextMenu(rcyvwAccountTransactions) // this is actually bad, splits code as context menu is created in AccountTransactionAdapter

        initLogic()

        return root
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        menu?.let {
            mnitmBalance = menu.findItem(R.id.mnitmBalance)

            mnitmSearchTransactions = menu.findItem(R.id.mnitmSearchTransactions)
            mnitmUpdateTransactions = menu.findItem(R.id.mnitmUpdateTransactions)

            initSearchView()

            initLogicAfterUiInitialized()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mnitmUpdateTransactions -> {
                updateAccountsTransactions()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initSearchView() {
        context?.asActivity()?.let { context ->
            (context.getSystemService(Context.SEARCH_SERVICE) as? SearchManager)?.let { searchManager ->
                (mnitmSearchTransactions.actionView as? SearchView)?.let { searchView ->
                    searchView.setSearchableInfo(searchManager.getSearchableInfo(context.componentName))

                    // if imeOptions aren't set like this searchView would take whole remaining screen when focused in landscape mode (see https://stackoverflow.com/questions/15296129/searchview-and-keyboard)
                    val searchInput =
                        searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text) as? EditText
                    searchInput?.imeOptions = EditorInfo.IME_ACTION_SEARCH or EditorInfo.IME_FLAG_NO_EXTRACT_UI

                    searchView.setOnQueryTextListener(searchAccountTransactionsTextListener)
                }
            }
        }
    }


    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mnitmShowTransferMoneyDialog -> {
                showTransferMoneyDialog()
                return true
            }
        }

        return super.onContextItemSelected(item)
    }


    private fun initLogic() {

        // TODO: this is such a bad code style
        (context as? MainActivity)?.presenter?.let { presenter ->
            this.presenter = presenter
        }
    }

    private fun initLogicAfterUiInitialized() {
        presenter.addSelectedBankAccountsChangedListener { handleSelectedBankAccountsChanged() }

        presenter.addRetrievedAccountTransactionsResponseListener { _, response ->
            handleGetTransactionsResponse(response)
        }

        updateTransactionsToDisplayOnUiThread()
    }


    private fun handleSelectedBankAccountsChanged() {
        context?.asActivity()?.let { activity ->
            activity.runOnUiThread {
                mnitmSearchTransactions.isVisible = presenter.doSelectedBankAccountsSupportRetrievingAccountTransactions
                mnitmUpdateTransactions.isVisible = presenter.doSelectedBankAccountsSupportRetrievingAccountTransactions

                updateTransactionsToDisplayOnUiThread()
            }
        }
    }

    private fun updateAccountsTransactions() {
        presenter.updateAccountsTransactionsAsync { }
    }

    private fun handleGetTransactionsResponse(response: GetTransactionsResponse) {
        context?.asActivity()?.let { activity ->
            activity.runOnUiThread {
                if (response.isSuccessful) {
                    updateTransactionsToDisplayOnUiThread()
                }
                else {
                    AlertDialog.Builder(activity) // TODO: may show account name in message
                        .setMessage(activity.getString(R.string.fragment_home_could_not_retrieve_account_transactions, response.errorToShowToUser))
                        .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
    }


    private fun showTransferMoneyDialog() {
        transactionAdapter.selectedTransaction?.let { selectedTransaction ->
            presenter.showTransferMoneyDialog(selectedTransaction.bankAccount, mapPreselectedValues(selectedTransaction))
        }
    }

    private fun mapPreselectedValues(selectedTransaction: AccountTransaction?): TransferMoneyData? {
        selectedTransaction?.let {
            return TransferMoneyData.fromAccountTransaction(selectedTransaction)
        }

        return null
    }


    private val searchAccountTransactionsTextListener: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextChange(query: String): Boolean {
            appliedTransactionsFilter = query

            updateTransactionsToDisplayOnUiThread()

            return true
        }

        override fun onQueryTextSubmit(query: String): Boolean {
            return true
        }
    }


    private fun updateTransactionsToDisplayOnUiThread() {
        transactionAdapter.items = presenter.searchSelectedAccountTransactions(appliedTransactionsFilter)

        // TODO: if transactions are filtered calculate and show balance of displayed transactions?
        mnitmBalance.title = presenter.balanceOfSelectedBankAccounts.toString()
        mnitmBalance.isVisible = presenter.doSelectedBankAccountsSupportRetrievingBalance
    }

}