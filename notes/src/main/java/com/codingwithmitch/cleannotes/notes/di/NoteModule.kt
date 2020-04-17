package com.codingwithmitch.notes.di

import androidx.room.Room
import androidx.work.WorkManager
import com.codingwithmitch.cleannotes.core.di.scopes.FeatureScope
import com.codingwithmitch.cleannotes.di.features.notes.NotesFeature
import com.codingwithmitch.cleannotes.notes.business.data.datasource.NoteCacheDataSource
import com.codingwithmitch.cleannotes.notes.business.data.repository.NoteRepositoryImpl
import com.codingwithmitch.cleannotes.notes.framework.datasource.mappers.NoteEntityMapper
import com.codingwithmitch.cleannotes.notes.di.NotesFeatureImpl
import com.codingwithmitch.cleannotes.notes.business.domain.repository.NoteRepository
import com.codingwithmitch.cleannotes.presentation.BaseApplication
import com.codingwithmitch.cleannotes.core.business.DateUtil
import com.codingwithmitch.cleannotes.notes.business.interactors.notedetail.NoteDetailInteractors
import com.codingwithmitch.cleannotes.notes.business.interactors.notedetail.UpdateNote
import com.codingwithmitch.cleannotes.notes.business.interactors.notelist.*
import com.codingwithmitch.cleannotes.notes.framework.datasource.mappers.NoteFactory
import com.codingwithmitch.notes.datasource.cache.db.NoteDao
import com.codingwithmitch.notes.datasource.cache.db.NoteDatabase
import com.codingwithmitch.notes.datasource.cache.db.NoteDatabase.Companion.DATABASE_NAME
import com.codingwithmitch.notes.datasource.cache.repository.NoteCacheDataSourceImpl
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
@Module
object NoteModule {

    @FeatureScope
    @Provides
    @JvmStatic
    fun provideFeatureImpl(featureImpl: NotesFeatureImpl): NotesFeature {
        return featureImpl
    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteFactory(dateUtil: DateUtil): NoteFactory{
        return NoteFactory(dateUtil)
    }

//    @JvmStatic
//    @FeatureScope
//    @Provides
//    fun provideNoteFragmentFactory(someString: String): FragmentFactory{
//        return NotesFragmentFactory(someString)
//    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteDb(app: BaseApplication): NoteDatabase {
        return Room
//            .inMemoryDatabaseBuilder(app, NoteDatabase::class.java)
            .databaseBuilder(app, NoteDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteDAO(noteDatabase: NoteDatabase): NoteDao {
        return noteDatabase.noteDao()
    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteEntityMapper(dateUtil: DateUtil): NoteEntityMapper{
        return NoteEntityMapper(dateUtil)
    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteCacheDataSource(
        noteDao: NoteDao,
        noteEntityMapper: NoteEntityMapper,
        dateUtil: DateUtil
    ): NoteCacheDataSource {
        return NoteCacheDataSourceImpl(noteDao, noteEntityMapper, dateUtil)
    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteRepository(
        noteCacheDataSource: NoteCacheDataSource
    ): NoteRepository {
        return NoteRepositoryImpl(noteCacheDataSource)
    }


    @JvmStatic
    @FeatureScope
    @Provides
    fun provideWorkManager(baseApplication: BaseApplication): WorkManager {
        return WorkManager.getInstance(baseApplication)
    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteDetailInteractors(
        noteRepository: NoteRepository
    ): NoteDetailInteractors {
        return NoteDetailInteractors(
            UpdateNote(
                noteRepository
            )
        )
    }

//    @JvmStatic
//    @FeatureScope
//    @Provides
//    fun provideNoteListInteractors(
//        noteRepository: NoteRepository,
//        noteFactory: NoteFactory,
//        workManager: WorkManager
//    ): NoteListInteractors{
//        return NoteListInteractors(
//            InsertNewNote(noteRepository, noteFactory),
//            DeleteNote(noteRepository, workManager),
//            SearchNotes(noteRepository),
//            GetNumNotes(noteRepository)
//
//        )
//    }

    @JvmStatic
    @FeatureScope
    @Provides
    fun provideNoteListInteractors(
        noteRepository: NoteRepository,
        noteFactory: NoteFactory,
        workManager: WorkManager
    ): NoteListInteractors {
        return NoteListInteractors(
            InsertNewNote(noteRepository, noteFactory),
            DeleteNote(workManager, noteRepository),
            SearchNotes(noteRepository),
            GetNumNotes(noteRepository)
        )
    }

}
















