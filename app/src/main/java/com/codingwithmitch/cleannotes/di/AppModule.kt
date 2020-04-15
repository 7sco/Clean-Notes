package com.codingwithmitch.cleannotes.di

import androidx.work.WorkManager
import com.codingwithmitch.cleannotes.core.business.DateUtil
import com.codingwithmitch.cleannotes.core.di.scopes.FeatureScope
import com.codingwithmitch.cleannotes.presentation.BaseApplication
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object AppModule {


    @JvmStatic
    @Singleton
    @Provides
    fun provideDateUtil(): DateUtil {
        return DateUtil()
    }


}