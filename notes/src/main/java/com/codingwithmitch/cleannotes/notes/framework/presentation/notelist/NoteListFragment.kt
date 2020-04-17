package com.codingwithmitch.cleannotes.notes.framework.presentation.notelist

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.codingwithmitch.cleannotes.core.business.state.*
import com.codingwithmitch.cleannotes.core.framework.DialogInputCaptureCallback
import com.codingwithmitch.cleannotes.core.framework.TopSpacingItemDecoration
import com.codingwithmitch.cleannotes.core.framework.hideKeyboard
import com.codingwithmitch.cleannotes.core.util.printLogD
import com.codingwithmitch.cleannotes.notes.business.domain.model.Note
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote.Companion.DELETE_NOTE_FAILED
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote.Companion.DELETE_NOTE_JOB_TAG
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote.Companion.DELETE_NOTE_PENDING
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote.Companion.DELETE_NOTE_SHOW_UNDO_SNACKBAR
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote.Companion.DELETE_NOTE_STATE_MESSAGE
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.codingwithmitch.cleannotes.notes.framework.presentation.BaseNoteFragment
import com.codingwithmitch.cleannotes.notes.framework.presentation.notedetail.NOTE_DETAIL_SELECTED_NOTE_BUNDLE_KEY
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state.NoteListStateEvent.*
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state.NoteListViewState
import com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state.NoteListViewState.*
import com.codingwithmitch.notes.R
import kotlinx.android.synthetic.main.fragment_note_list.*
import kotlinx.coroutines.*
import javax.inject.Inject


const val NOTE_LIST_STATE_BUNDLE_KEY = "com.codingwithmitch.cleannotes.notes.framework.presentation.notelist.state"

@FlowPreview
@ExperimentalCoroutinesApi
class NoteListFragment : BaseNoteFragment(R.layout.fragment_note_list),
    NoteListAdapter.Interaction,
    ItemTouchHelperAdapter
{

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    val viewModel: NoteListViewModel by viewModels {
        viewModelFactory
    }

    private var listAdapter: NoteListAdapter? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setupChannel()
//        arguments?.let { args ->
//            args.getParcelable<Note>(NOTE_PENDING_DELETE_BUNDLE_KEY)?.let { note ->
//                viewModel.setNotePendingDelete(note)
//                viewModel.beginPendingDelete()
//            }
//        }
        arguments?.let { args ->
            args.getParcelable<Note>(NOTE_PENDING_DELETE_BUNDLE_KEY)?.let { note ->
                viewModel.beginPendingDelete(
                    note = note,
                    listPosition = viewModel.getNoteList().indexOf(note)
                )
                clearDeleteArgs()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()
        setupFAB()
        subscribeObservers()

        restoreInstanceState(savedInstanceState)
        setupWorkManagerJobObservers()
    }

    private fun setupWorkManagerJobObservers(){

        workManager
            .getWorkInfosForUniqueWorkLiveData(DELETE_NOTE_JOB_TAG)
            .observe(viewLifecycleOwner, Observer { workInfoList: MutableList<WorkInfo?> ->
                for(workInfo in workInfoList){
                    if (workInfo != null) {
                        val progress = workInfo.progress
                        if(workInfo.state == WorkInfo.State.RUNNING){
                            val stateMessage: String? = progress.getString(DELETE_NOTE_STATE_MESSAGE)
                            stateMessage?.let { message ->

                                when(message){

                                    DELETE_NOTE_SHOW_UNDO_SNACKBAR -> {
                                        showUndoSnackbar_deleteNote()
                                    }
                                }
                            }
                        }
                        if(workInfo.state == WorkInfo.State.SUCCEEDED){
                            onDeleteSuccess()
                        }
                        if(workInfo.state == WorkInfo.State.CANCELLED){
                            onDeleteFailed()
                        }

                        printLogD("NoteListFragment", "observer: ${progress}, state: ${workInfo.state}")
                    }
                }
            })
    }

    private fun onDeleteSuccess(){
        viewModel.onCompleteDelete()
    }

    private fun onDeleteFailed(){
        viewModel.undoDelete()
        viewModel.onCompleteDelete()
    }

    private fun showUndoSnackbar_deleteNote(){
        uiController.onResponseReceived(
            response = Response(
                message = DELETE_NOTE_PENDING,
                uiComponentType = UIComponentType.SnackBar(
                    object : SnackbarUndoCallback {
                        override fun undo() {
                            cancelWorkManagerJob(DELETE_NOTE_JOB_TAG)
                            onDeleteFailed()
                        }
                    }
                ),
                messageType = MessageType.Info()
            ),
            stateMessageCallback = object: StateMessageCallback{
                override fun removeMessageFromStack() {
                    // does nothing since not added to msg stack in this case
                }
            }
        )
    }

    private fun cancelWorkManagerJob(tag: String){
        workManager.cancelUniqueWork(tag)
    }

    override fun onResume() {
        super.onResume()
        viewModel.countNumNotesInCache()
        viewModel.restoreFromCache()
    }

    override fun onPause() {
        super.onPause()
        saveLayoutManagerState()
    }

    // return true only if state is restored
    private fun restoreInstanceState(savedInstanceState: Bundle?){
        savedInstanceState?.let { inState ->
            (inState[NOTE_LIST_STATE_BUNDLE_KEY] as NoteListViewState?)?.let { viewState ->
                viewModel.setViewState(viewState)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val viewState = viewModel.viewState.value

        //clear the list. Don't want to save a large list to bundle.
        viewState?.noteList =  ArrayList()

        outState.putParcelable(
            NOTE_LIST_STATE_BUNDLE_KEY,
            viewState
        )
        super.onSaveInstanceState(outState)
    }

    override fun restoreListPosition() {
        viewModel.viewState.value?.layoutManagerState?.let { lmState ->
            recycler_view?.layoutManager?.onRestoreInstanceState(lmState)
        }
    }

    private fun saveLayoutManagerState(){
        recycler_view.layoutManager?.onSaveInstanceState()?.let { lmState ->
            viewModel.setLayoutManagerState(lmState)
        }
    }

    private fun setupRecyclerView(){
        recycler_view.apply {
            layoutManager = LinearLayoutManager(activity)
            val topSpacingDecorator = TopSpacingItemDecoration(20)
            addItemDecoration(topSpacingDecorator)
            itemTouchHelper = ItemTouchHelper(
                NoteItemTouchHelperCallback(this@NoteListFragment)
            )
            listAdapter = NoteListAdapter(
                this@NoteListFragment,
                lifecycleScope,
                itemTouchHelper
            )
            itemTouchHelper?.attachToRecyclerView(this)
            addOnScrollListener(object: RecyclerView.OnScrollListener(){
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val lastPosition = layoutManager.findLastVisibleItemPosition()
                    if (lastPosition == listAdapter?.itemCount?.minus(1)) {
                        viewModel.nextPage()
                    }
                }
            })
            adapter = listAdapter
        }
    }

    private fun subscribeObservers(){
        viewModel.viewState.observe(viewLifecycleOwner, Observer{ viewState ->

            if(viewState != null){
                viewState.noteList?.let { noteList ->
                    if(viewModel.isPaginationExhausted()
                        && !viewModel.isQueryExhausted()){
                        viewModel.setQueryExhausted(true)
                    }
                    listAdapter?.submitList(noteList)
                    listAdapter?.notifyDataSetChanged()
                }

                // a note been inserted or selected
                viewState.newNote?.let { newNote ->
                    navigateToDetailFragment(newNote)
                }

                viewState.notePendingDelete?.let { note ->
                    viewModel.removePendingNoteFromList(note.note)
                }
            }
        })

        viewModel.shouldDisplayProgressBar.observe(viewLifecycleOwner, Observer {
            printActiveJobs()
            uiController.displayProgressBar(it)
        })

        viewModel.stateMessage.observe(viewLifecycleOwner, Observer { stateMessage ->

            if(stateMessage != null){

                uiController.onResponseReceived(
                    response = stateMessage.response,
                    stateMessageCallback = object: StateMessageCallback {
                        override fun removeMessageFromStack() {
                            viewModel.clearStateMessage()
                        }
                    }
                )
            }


        })
    }

    // for debugging
    private fun printActiveJobs(){
        for((index, job) in viewModel.getActiveJobs().withIndex()){
            printLogD("NoteList",
                "${index}: ${job}")
        }
    }

//    private fun onCompleteDelete(){
//        clearDeleteArgs()
//        viewModel.onCompleteDelete()
//    }

    private fun clearDeleteArgs(){
        arguments?.remove(NOTE_PENDING_DELETE_BUNDLE_KEY)
    }

    private fun navigateToDetailFragment(selectedNote: Note){
        val bundle = bundleOf(NOTE_DETAIL_SELECTED_NOTE_BUNDLE_KEY to selectedNote)
        findNavController().navigate(
            R.id.action_note_list_fragment_to_noteDetailFragment,
            bundle
        )
        viewModel.setNote(null)
    }

    private fun setupUI(){
        view?.hideKeyboard()
        uiController.checkBottomNav(getString(com.codingwithmitch.cleannotes.R.string.module_notes_name))
        uiController.displayBottomNav(true)
    }

    override fun inject() {
        getNoteComponent()?.inject(this)
    }

    override fun onItemSelected(position: Int, item: Note) {
        viewModel.setNote(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listAdapter = null // can leak memory
        itemTouchHelper = null // can leak memory
    }

//    override fun onItemSwiped(position: Int) {
//        if(!viewModel.isDeletePending()){
//            viewModel.setNotePendingDelete(listAdapter?.getNote(position))
//            viewModel.beginPendingDelete()
//        }
//        else{
//            listAdapter?.notifyDataSetChanged()
//        }
//    }

    override fun onItemSwiped(position: Int) {
        listAdapter?.getNote(position)?.let { note ->
            viewModel.beginPendingDelete(
                note = note,
                listPosition = position
            )
        }
    }

    private fun setupSearchView(){

        val searchPlate: SearchView.SearchAutoComplete?
                = search_view.findViewById(androidx.appcompat.R.id.search_src_text)

        searchPlate?.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED
                || actionId == EditorInfo.IME_ACTION_SEARCH ) {
                val searchQuery = v.text.toString()
                viewModel.setQuery(searchQuery)
                startNewSearch()
            }
            true
        }
    }

    private fun setupFAB(){
        add_new_note_fab.setOnClickListener {
            uiController.displayInputCaptureDialog(
                getString(com.codingwithmitch.cleannotes.R.string.text_enter_a_title),
                object: DialogInputCaptureCallback{
                    override fun onTextCaptured(text: String) {
                        val newNote = viewModel.createNewNote(title = text)
                        viewModel.setStateEvent(
                            InsertNewNoteEvent(
                                title = newNote.title,
                                body = ""
                            )
                        )
                    }
                }
            )
        }
    }

    private fun startNewSearch() = viewModel.loadFirstPage()

    private fun setupSwipeRefresh(){
        swipe_refresh.setOnRefreshListener {
            startNewSearch()
            swipe_refresh.isRefreshing = false
        }
    }

//    companion object WorkManagerConstants {

//        const val DELETE_NOTE_JOB_TAG = "delete_note"
//        const val STATE_MESSAGE = "state_message"
//        const val SHOW_UNDO_SNACKBAR = "show_undo_snackbar"
//        const val DELETE_NOTE_PENDING = "Delete pending..."
//    }
    
}































