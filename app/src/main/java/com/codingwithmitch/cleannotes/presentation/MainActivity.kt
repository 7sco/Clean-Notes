package com.codingwithmitch.cleannotes.presentation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.input.input
import com.codingwithmitch.cleannotes.R
import com.codingwithmitch.cleannotes.core.business.state.*
import com.codingwithmitch.cleannotes.core.business.state.UIComponentType.*
import com.codingwithmitch.cleannotes.core.framework.*
import com.codingwithmitch.cleannotes.core.util.printLogD
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(),
    UIController
{

    private val TAG: String = "AppDebug"

    private val bottomNavView: BottomNavigationView by lazy {
        findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
    }
    private var appBarConfiguration: AppBarConfiguration? = null

    private val topLevelFragmentIds = ArrayList<Int>()

    private var dialogInView: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        retrieveTopLevelFragmentIds()
        setupBottomNavigation()

//        setupWorkManagerJobObservers()
    }

    companion object WorkManagerConstants {

        const val DELETE_NOTE_JOB_TAG = "delete_note"
        const val STATE_MESSAGE = "state_message"
        const val SHOW_UNDO_SNACKBAR = "show_undo_snackbar"
        const val DELETE_NOTE_PENDING = "Delete pending..."
    }


//    private fun setupWorkManagerJobObservers(){
//        WorkManager.getInstance(applicationContext)
//            .getWorkInfosByTagLiveData(DELETE_NOTE_JOB_TAG)
//            .observe(this, Observer { workInfoList: MutableList<WorkInfo?> ->
//                for(workInfo in workInfoList){
//                    if (workInfo != null) {
//                        if(workInfo.state == WorkInfo.State.RUNNING){
//                            val progress = workInfo.progress
//                            val stateMessage: String? = progress.getString(STATE_MESSAGE)
//                            printLogD("WorkManager: (observer)", "stateMessage: ${stateMessage}")
//                            stateMessage?.let { message ->
//                                if(message.equals(SHOW_UNDO_SNACKBAR)){
//                                    showUndoSnackbar_deleteNote()
//                                }
//                            }
//                            val value = progress.getInt("Progress", 0)
//                            printLogD("WorkManager: (observer)", "progressValue: ${value}")
//                            printLogD("WorkManager: (observer)", "outputData: ${workInfo.outputData}")
//                            printLogD("WorkManager: (observer)", "------------\n\n")
//                        }
//                    }
//                }
//            })
//    }

//    private fun cancelWorkManagerJob(tag: String){
//        WorkManager.getInstance(applicationContext)
//            .cancelAllWorkByTag(tag)
//    }

//    private fun showUndoSnackbar_deleteNote(){
//        onResponseReceived(
//            response = Response(
//                message = DELETE_NOTE_PENDING,
//                uiComponentType = SnackBar(
//                    object: SnackbarUndoCallback{
//                        override fun undo() {
//                            cancelWorkManagerJob(DELETE_NOTE_JOB_TAG)
//                        }
//                    }
//                ),
//                messageType = MessageType.Info()
//            ),
//            stateMessageCallback = object: StateMessageCallback{
//                override fun removeMessageFromStack() {
//                    // does nothing since not added to msg stack in this case
//                }
//            }
//        )
//    }


    private fun retrieveTopLevelFragmentIds(){
        initializeNotesFeature()
        initializeRemindersFeature()
    }

    fun initializeRemindersFeature() {
        val remindersModule = (application as BaseApplication).appComponent
            .remindersFeature()
        if (remindersModule != null) {
            topLevelFragmentIds.add(remindersModule.provideTopLevelFragmentId())
        }
    }

    fun initializeNotesFeature() {
        val notesModule = (application as BaseApplication).appComponent
            .notesFeature()
        if (notesModule != null) {
            topLevelFragmentIds.add(notesModule.provideTopLevelFragmentId())
        }
    }

    private fun setupBottomNavigation(){
        val navController = findNavController(R.id.nav_host_fragment)
        bottomNavView.setupWithNavController(navController)
        bottomNavView.setOnNavigationItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem.itemId)
        }
    }

    private fun onNavigationItemSelected(menuItemId: Int): Boolean{
        when(menuItemId){

            R.id.menu_nav_notes -> {
                navNotesGraph()
            }

            R.id.menu_nav_reminders -> {
                navRemindersGraph()
            }
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_nav_settings -> {
                navSettingsGraph()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun displayBottomNav(isDisplayed: Boolean) {
        if(isDisplayed)
            bottom_navigation_view.visible()
        else
            bottom_navigation_view.gone()
    }


    override fun displayProgressBar(isDisplayed: Boolean) {
        if(isDisplayed)
            main_progress_bar.visible()
        else
            main_progress_bar.gone()
    }

    override fun navNotesGraph() {
        findNavController(R.id.nav_host_fragment).navigate(R.id.nav_notes_graph)
    }

    override fun navSettingsGraph() {
        findNavController(R.id.nav_host_fragment).navigate(R.id.nav_settings_graph)
    }

    override fun navRemindersGraph() {
        findNavController(R.id.nav_host_fragment).navigate(R.id.nav_reminders_graph)
    }

    override fun checkBottomNav(moduleName: String) {
        when(moduleName){

            getString(R.string.module_notes_name) ->{
                bottomNavView.menu.findItem(R.id.menu_nav_notes).isChecked = true
            }

            getString(R.string.module_reminders_name) ->{
                bottomNavView.menu.findItem(R.id.menu_nav_reminders).isChecked = true
            }

        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment)
            .navigateUp(appBarConfiguration as AppBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun displayInputCaptureDialog(
        title: String,
        callback: DialogInputCaptureCallback
    ) {
        MaterialDialog(this).show {
            title(text = title)
            input(waitForPositiveButton = true){ _, text ->
                callback.onTextCaptured(text.toString())
            }
            positiveButton(R.string.text_ok)
            onDismiss {
                dialogInView = null
            }
            cancelable(true)
        }
    }

    override fun onResponseReceived(
        response: Response,
        stateMessageCallback: StateMessageCallback
    ) {

        when(response.uiComponentType){

            is SnackBar -> {
                (response.uiComponentType as SnackBar).undoCallback?.let { callback ->
                    response.message?.let { msg ->
                        displaySnackbar(
                            message = msg,
                            snackbarUndoCallback = callback,
                            stateMessageCallback = stateMessageCallback
                        )
                    }
                }
            }

            is AreYouSureDialog -> {

                response.message?.let {
                    areYouSureDialog(
                        message = it,
                        callback = (response.uiComponentType as AreYouSureDialog).callback,
                        stateMessageCallback = stateMessageCallback
                    )
                }
            }

            is Toast -> {
                response.message?.let {
                    displayToast(
                        message = it,
                        stateMessageCallback = stateMessageCallback
                    )
                }
            }

            is Dialog -> {
                displayDialog(
                    response = response,
                    stateMessageCallback = stateMessageCallback
                )
            }

            is None -> {
                // This would be a good place to send to your Error Reporting
                // software of choice (ex: Firebase crash reporting)
                Log.i(TAG, "onResponseReceived: ${response.message}")
                stateMessageCallback.removeMessageFromStack()
            }
        }
    }

    private fun displaySnackbar(
        message: String,
        snackbarUndoCallback: SnackbarUndoCallback,
        stateMessageCallback: StateMessageCallback
    ){
        val snackbar = Snackbar.make(
            findViewById(R.id.main_container),
            message,
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(
            getString(R.string.text_undo),
            SnackbarUndoListener(snackbarUndoCallback)
        )
        snackbar.show()
        stateMessageCallback.removeMessageFromStack()
    }

    private fun displayDialog(
        response: Response,
        stateMessageCallback: StateMessageCallback
    ){
        response.message?.let { message ->

            dialogInView = when (response.messageType) {

                is MessageType.Error -> {
                    displayErrorDialog(
                        message = message,
                        stateMessageCallback = stateMessageCallback
                    )
                }

                is MessageType.Success -> {
                    displaySuccessDialog(
                        message = message,
                        stateMessageCallback = stateMessageCallback
                    )
                }

                is MessageType.Info -> {
                    displayInfoDialog(
                        message = message,
                        stateMessageCallback = stateMessageCallback
                    )
                }

                else -> {
                    // do nothing
                    stateMessageCallback.removeMessageFromStack()
                    null
                }
            }
        }?: stateMessageCallback.removeMessageFromStack()
    }

    override fun hideSoftKeyboard() {
        if (currentFocus != null) {
            val inputMethodManager = getSystemService(
                Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager
                .hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        if(dialogInView != null){
            (dialogInView as MaterialDialog).dismiss()
            dialogInView = null
        }
    }

    private fun displaySuccessDialog(
        message: String?,
        stateMessageCallback: StateMessageCallback
    ): MaterialDialog {
        return MaterialDialog(this)
            .show{
                title(R.string.text_success)
                message(text = message)
                positiveButton(R.string.text_ok){
                    stateMessageCallback.removeMessageFromStack()
                    dismiss()
                }
                onDismiss {
                    dialogInView = null
                }
                cancelable(false)
            }
    }

    private fun displayErrorDialog(
        message: String?,
        stateMessageCallback: StateMessageCallback
    ): MaterialDialog {
        return MaterialDialog(this)
            .show{
                title(R.string.text_error)
                message(text = message)
                positiveButton(R.string.text_ok){
                    stateMessageCallback.removeMessageFromStack()
                    dismiss()
                }
                onDismiss {
                    dialogInView = null
                }
                cancelable(false)
            }
    }

    private fun displayInfoDialog(
        message: String?,
        stateMessageCallback: StateMessageCallback
    ): MaterialDialog {
        return MaterialDialog(this)
            .show{
                title(R.string.text_info)
                message(text = message)
                positiveButton(R.string.text_ok){
                    stateMessageCallback.removeMessageFromStack()
                    dismiss()
                }
                onDismiss {
                    dialogInView = null
                }
                cancelable(false)
            }
    }

    private fun areYouSureDialog(
        message: String,
        callback: AreYouSureCallback,
        stateMessageCallback: StateMessageCallback
    ): MaterialDialog {
        return MaterialDialog(this)
            .show{
                title(R.string.are_you_sure)
                message(text = message)
                negativeButton(R.string.text_cancel){
                    stateMessageCallback.removeMessageFromStack()
                    callback.cancel()
                    dismiss()
                }
                positiveButton(R.string.text_yes){
                    stateMessageCallback.removeMessageFromStack()
                    callback.proceed()
                    dismiss()
                }
                onDismiss {
                    dialogInView = null
                }
                cancelable(false)
            }
    }

}

























