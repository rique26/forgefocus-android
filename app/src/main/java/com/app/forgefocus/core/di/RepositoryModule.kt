package com.app.forgefocus.core.di

import com.app.forgefocus.features.mountains.data.GoalRepositoryImpl
import com.app.forgefocus.features.mountains.domain.GoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    @Singleton
    fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository
}