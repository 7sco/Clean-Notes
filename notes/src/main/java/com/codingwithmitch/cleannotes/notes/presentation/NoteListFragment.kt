package com.codingwithmitch.cleannotes.notes.presentation

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.codingwithmitch.cleannotes.presentation.MainActivity
import com.codingwithmitch.cleannotes.presentation.MainNavController
import com.codingwithmitch.notes.R
import kotlinx.android.synthetic.main.fragment_note_list.*
import java.lang.ClassCastException


class NoteListFragment : Fragment(R.layout.fragment_note_list) {

    lateinit var navController: MainNavController

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        notes_title.setOnClickListener {
            findNavController().navigate(R.id.action_note_list_fragment_to_noteDetailFragment)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try{
            navController = context as MainActivity
        }catch (e: ClassCastException){
            e.printStackTrace()
        }
    }


}














