package com.app.forgefocus.features.mountains.di

import com.app.forgefocus.core.domain.usecase.BreakMountainBlockUseCase
import com.app.forgefocus.core.domain.usecase.BreakMountainBlockUseCaseImpl
import com.app.forgefocus.core.domain.usecase.CreateGoalUseCase
import com.app.forgefocus.core.domain.usecase.CreateGoalUseCaseImpl
import com.app.forgefocus.core.domain.usecase.DeleteGoalUseCase
import com.app.forgefocus.core.domain.usecase.DeleteGoalUseCaseImpl
import com.app.forgefocus.core.domain.usecase.GetDailyProgressUseCase
import com.app.forgefocus.core.domain.usecase.GetDailyProgressUseCaseImpl
import com.app.forgefocus.core.domain.usecase.GetGoalsUseCase
import com.app.forgefocus.core.domain.usecase.GetGoalsUseCaseImpl
import com.app.forgefocus.core.domain.usecase.GetProgressLogsUseCase
import com.app.forgefocus.core.domain.usecase.GetProgressLogsUseCaseImpl
import com.app.forgefocus.features.mountains.data.GoalRepositoryImpl
import com.app.forgefocus.features.mountains.domain.GetDashboardDataUseCase
import com.app.forgefocus.features.mountains.domain.GetDashboardDataUseCaseImpl
import com.app.forgefocus.features.mountains.domain.GoalRepository
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val mountainsModule = module {
    // Data Layer
    singleOf(::GoalRepositoryImpl) bind GoalRepository::class

    // Domain Layer
    factoryOf(::GetDashboardDataUseCaseImpl) bind GetDashboardDataUseCase::class
    factoryOf(::BreakMountainBlockUseCaseImpl) bind BreakMountainBlockUseCase::class
    factoryOf(::CreateGoalUseCaseImpl) bind CreateGoalUseCase::class
    factoryOf(::DeleteGoalUseCaseImpl) bind DeleteGoalUseCase::class
    factoryOf(::GetDailyProgressUseCaseImpl) bind GetDailyProgressUseCase::class
    factoryOf(::GetGoalsUseCaseImpl) bind GetGoalsUseCase::class
    factoryOf(::GetProgressLogsUseCaseImpl) bind GetProgressLogsUseCase::class

    // Presentation Layer
    viewModelOf(::DashboardViewModel)
}