package com.app.forgefocus.features.mountains.di

import com.app.forgefocus.features.mountains.data.GoalRepositoryImpl
import com.app.forgefocus.features.mountains.domain.GetDashboardDataUseCase
import com.app.forgefocus.features.mountains.domain.GoalRepository
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel
import com.app.forgefocus.core.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf

val mountainsModule = module {
    // Data Layer
    singleOf(::GoalRepositoryImpl) bind GoalRepository::class

    // Domain Layer
    factoryOf(::GetDashboardDataUseCase)
    factoryOf(::BreakMountainBlockUseCase)
    factoryOf(::CreateGoalUseCase)
    factoryOf(::DeleteGoalUseCase)
    factoryOf(::GetDailyProgressUseCase)
    factoryOf(::GetGoalsUseCase)
    factoryOf(::GetProgressLogsUseCase)

    // Presentation Layer
    viewModelOf(::DashboardViewModel)
}