package com.codingwithmitch.cleannotes.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.codingwithmitch.cleannotes.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.RuntimeException


class MainActivity : AppCompatActivity(),
    MainNavController,
    UIController
{

    private val TAG: String = "AppDebug"

    private val appBackStack: AppBackStack by lazy {
        AppBackStack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActionBar()
        setupBottomNavigation()

        subscribeObservers()
    }

    private fun subscribeObservers(){
        appBackStack.topFragment.observe(this, Observer { topFragmentTag ->
            when(topFragmentTag){

                "NoteListFragment" -> {
                    // deep link to NoteListFragment
                }

                "NoteDetailFragment" -> {

                }

                "SettingsFragment" -> {

                }
            }
        })
    }

    private fun setupBottomNavigation(){
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)

        bottomNavView.setOnNavigationItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem.itemId)
        }
    }


    class AppBackStack(): SparseArray<ModuleBackStack>(){

        private val _topFragment: MutableLiveData<String> = MutableLiveData()

        val topFragment: LiveData<String>
                get() = _topFragment

        // top level backstack (Bottom nav)
        // Strictly for keeping track of order
        private val appBackStack: LinkedHashSet<Int> = LinkedHashSet()

        fun appendFragment(menuItemId: Int, fragmentTag: String){
            val moduleBackStack = get(menuItemId)?: ModuleBackStack()
            val transaction = moduleBackStack.add(fragmentTag)
            if(transaction){ // prevent duplicate transactions
                put(menuItemId, moduleBackStack)
                setTopFragment(fragmentTag)
                appBackStack.add(menuItemId)
            }
        }

        fun popBackStack(): Boolean {
            if(appBackStack.size > 0){
                val topItemId: Int = appBackStack.toIntArray()[appBackStack.size - 1]
                val topModule = get(topItemId)

                // remove top fragment from stack
                topModule.popBackStack()

                // return next fragment in line
                if(topModule.getTopFragment() != null){
                    put(topItemId, topModule)
                    setTopFragment(topModule.getTopFragment() as String)
                    return true
                }
                else{ // Look to next module in line (if it exists)
                    remove(topItemId)
                    appBackStack.remove(topItemId)
                    return popBackStack()
                }
            }
            else{
                return false // stack is empty. close app
            }
        }

        private fun changeGraphs(newMenuItemId: Int){

        }

        fun onSelectMenuItem(menuItemId: Int){
            // check if already exists in appBackStack
            // if it does, setTopFragment

            // if is doesn't, appendFragment
        }

        private fun setTopFragment(tag: String){
            _topFragment.value = tag
        }

    }


    class ModuleBackStack: LinkedHashSet<String>(){

        fun getTopFragment(): String? {
            if(size > 0)
                return toArray()[size - 1] as String
            else
                return null
        }

        fun popBackStack(): Boolean {
            return if(size > 0)
                remove(toArray()[ size-1 ] )
            else
                false
        }

    }


//
//    fun playing(){
//
//        val notesFragments = ArrayList<String>()
//        notesFragments.add("n1")
//        notesFragments.add("n2")
//
//        val settingsFragments = ArrayList<String>()
//        settingsFragments.add("s1")
//
//        val remindersFragments = ArrayList<String>()
//        remindersFragments.add("r1")
//
//        val appBackStack = AppBackStack()
//        appBackStack.put(1, notesFragments)
//
//
//    }

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

    override fun navNotesGraph() {
        findNavController(R.id.nav_host_fragment).navigate(R.id.nav_notes_graph)
    }

    override fun navSettingsGraph() {
        findNavController(R.id.nav_host_fragment).navigate(R.id.nav_settings_graph)
    }

    override fun navRemindersGraph() {
        findNavController(R.id.nav_host_fragment).navigate(R.id.nav_reminders_graph)
    }

    private fun setupActionBar(){
        setSupportActionBar(tool_bar)
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
            bottom_navigation_view.visibility = View.VISIBLE
        else
            bottom_navigation_view.visibility = View.GONE
    }

    override fun displayProgressBar(isDisplayed: Boolean) {
        if(isDisplayed)
            main_progress_bar.visibility = View.VISIBLE
        else
            main_progress_bar.visibility = View.GONE
    }
}

























