package com.quizmaker.android;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.quizmaker.android.core.navigation.SessionViewModel;
import com.quizmaker.android.core.navigation.SessionViewModel_HiltModules;
import com.quizmaker.android.core.navigation.SessionViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.core.navigation.SessionViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.core.supabase.SupabaseModule_ProvideSupabaseClientFactory;
import com.quizmaker.android.repository.AiQuizRepository;
import com.quizmaker.android.repository.AuthRepository;
import com.quizmaker.android.repository.LeaderboardRepository;
import com.quizmaker.android.repository.ProfileRepository;
import com.quizmaker.android.repository.QuestionRepository;
import com.quizmaker.android.repository.QuizAnalysisRepository;
import com.quizmaker.android.repository.QuizDetailViewRepository;
import com.quizmaker.android.repository.QuizRepository;
import com.quizmaker.android.repository.QuizTakingRepository;
import com.quizmaker.android.repository.ResponseDetailRepository;
import com.quizmaker.android.ui.aiquiz.AiQuizViewModel;
import com.quizmaker.android.ui.aiquiz.AiQuizViewModel_HiltModules;
import com.quizmaker.android.ui.aiquiz.AiQuizViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.aiquiz.AiQuizViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.auth.AuthViewModel;
import com.quizmaker.android.ui.auth.AuthViewModel_HiltModules;
import com.quizmaker.android.ui.auth.AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.auth.AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.dashboard.DashboardViewModel;
import com.quizmaker.android.ui.dashboard.DashboardViewModel_HiltModules;
import com.quizmaker.android.ui.dashboard.DashboardViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.dashboard.DashboardViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.leaderboard.LeaderboardViewModel;
import com.quizmaker.android.ui.leaderboard.LeaderboardViewModel_HiltModules;
import com.quizmaker.android.ui.leaderboard.LeaderboardViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.leaderboard.LeaderboardViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.masterpaper.MasterPaperViewModel;
import com.quizmaker.android.ui.masterpaper.MasterPaperViewModel_HiltModules;
import com.quizmaker.android.ui.masterpaper.MasterPaperViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.masterpaper.MasterPaperViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.more.MoreViewModel;
import com.quizmaker.android.ui.more.MoreViewModel_HiltModules;
import com.quizmaker.android.ui.more.MoreViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.more.MoreViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.profile.ProfileViewModel;
import com.quizmaker.android.ui.profile.ProfileViewModel_HiltModules;
import com.quizmaker.android.ui.profile.ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.profile.ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.questionbank.QuestionBankViewModel;
import com.quizmaker.android.ui.questionbank.QuestionBankViewModel_HiltModules;
import com.quizmaker.android.ui.questionbank.QuestionBankViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.questionbank.QuestionBankViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.quizanalysis.QuizAnalysisViewModel;
import com.quizmaker.android.ui.quizanalysis.QuizAnalysisViewModel_HiltModules;
import com.quizmaker.android.ui.quizanalysis.QuizAnalysisViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.quizanalysis.QuizAnalysisViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.quizcreate.CreateQuizViewModel;
import com.quizmaker.android.ui.quizcreate.CreateQuizViewModel_HiltModules;
import com.quizmaker.android.ui.quizcreate.CreateQuizViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.quizcreate.CreateQuizViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.quizdetail.QuizDetailViewModel;
import com.quizmaker.android.ui.quizdetail.QuizDetailViewModel_HiltModules;
import com.quizmaker.android.ui.quizdetail.QuizDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.quizdetail.QuizDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.quizdetailview.QuizDetailViewViewModel;
import com.quizmaker.android.ui.quizdetailview.QuizDetailViewViewModel_HiltModules;
import com.quizmaker.android.ui.quizdetailview.QuizDetailViewViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.quizdetailview.QuizDetailViewViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.quizlist.QuizListViewModel;
import com.quizmaker.android.ui.quizlist.QuizListViewModel_HiltModules;
import com.quizmaker.android.ui.quizlist.QuizListViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.quizlist.QuizListViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.responsedetail.ResponseDetailViewModel;
import com.quizmaker.android.ui.responsedetail.ResponseDetailViewModel_HiltModules;
import com.quizmaker.android.ui.responsedetail.ResponseDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.responsedetail.ResponseDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.responses.ResponsesViewModel;
import com.quizmaker.android.ui.responses.ResponsesViewModel_HiltModules;
import com.quizmaker.android.ui.responses.ResponsesViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.responses.ResponsesViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.quizmaker.android.ui.takequiz.TakeQuizViewModel;
import com.quizmaker.android.ui.takequiz.TakeQuizViewModel_HiltModules;
import com.quizmaker.android.ui.takequiz.TakeQuizViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.quizmaker.android.ui.takequiz.TakeQuizViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import io.github.jan.supabase.SupabaseClient;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DaggerQuizMakerApp_HiltComponents_SingletonC {
  private DaggerQuizMakerApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static QuizMakerApp_HiltComponents.SingletonC create() {
    return new Builder().build();
  }

  public static final class Builder {
    private Builder() {
    }

    /**
     * @deprecated This module is declared, but an instance is not used in the component. This method is a no-op. For more, see https://dagger.dev/unused-modules.
     */
    @Deprecated
    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public QuizMakerApp_HiltComponents.SingletonC build() {
      return new SingletonCImpl();
    }
  }

  private static final class ActivityRetainedCBuilder implements QuizMakerApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public QuizMakerApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements QuizMakerApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public QuizMakerApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements QuizMakerApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public QuizMakerApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements QuizMakerApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public QuizMakerApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements QuizMakerApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public QuizMakerApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements QuizMakerApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public QuizMakerApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements QuizMakerApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public QuizMakerApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends QuizMakerApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends QuizMakerApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    FragmentCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends QuizMakerApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends QuizMakerApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    ActivityCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(17).put(AiQuizViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AiQuizViewModel_HiltModules.KeyModule.provide()).put(AuthViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AuthViewModel_HiltModules.KeyModule.provide()).put(CreateQuizViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CreateQuizViewModel_HiltModules.KeyModule.provide()).put(DashboardViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DashboardViewModel_HiltModules.KeyModule.provide()).put(LeaderboardViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, LeaderboardViewModel_HiltModules.KeyModule.provide()).put(MasterPaperViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, MasterPaperViewModel_HiltModules.KeyModule.provide()).put(MoreViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, MoreViewModel_HiltModules.KeyModule.provide()).put(ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ProfileViewModel_HiltModules.KeyModule.provide()).put(QuestionBankViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, QuestionBankViewModel_HiltModules.KeyModule.provide()).put(QuizAnalysisViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, QuizAnalysisViewModel_HiltModules.KeyModule.provide()).put(QuizDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, QuizDetailViewModel_HiltModules.KeyModule.provide()).put(QuizDetailViewViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, QuizDetailViewViewModel_HiltModules.KeyModule.provide()).put(QuizListViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, QuizListViewModel_HiltModules.KeyModule.provide()).put(ResponseDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ResponseDetailViewModel_HiltModules.KeyModule.provide()).put(ResponsesViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ResponsesViewModel_HiltModules.KeyModule.provide()).put(SessionViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SessionViewModel_HiltModules.KeyModule.provide()).put(TakeQuizViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, TakeQuizViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends QuizMakerApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    Provider<AiQuizViewModel> aiQuizViewModelProvider;

    Provider<AuthViewModel> authViewModelProvider;

    Provider<CreateQuizViewModel> createQuizViewModelProvider;

    Provider<DashboardViewModel> dashboardViewModelProvider;

    Provider<LeaderboardViewModel> leaderboardViewModelProvider;

    Provider<MasterPaperViewModel> masterPaperViewModelProvider;

    Provider<MoreViewModel> moreViewModelProvider;

    Provider<ProfileViewModel> profileViewModelProvider;

    Provider<QuestionBankViewModel> questionBankViewModelProvider;

    Provider<QuizAnalysisViewModel> quizAnalysisViewModelProvider;

    Provider<QuizDetailViewModel> quizDetailViewModelProvider;

    Provider<QuizDetailViewViewModel> quizDetailViewViewModelProvider;

    Provider<QuizListViewModel> quizListViewModelProvider;

    Provider<ResponseDetailViewModel> responseDetailViewModelProvider;

    Provider<ResponsesViewModel> responsesViewModelProvider;

    Provider<SessionViewModel> sessionViewModelProvider;

    Provider<TakeQuizViewModel> takeQuizViewModelProvider;

    ViewModelCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        SavedStateHandle savedStateHandleParam, ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.aiQuizViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.authViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.createQuizViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.leaderboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.masterPaperViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.moreViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.questionBankViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.quizAnalysisViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.quizDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.quizDetailViewViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.quizListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.responseDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.responsesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.sessionViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
      this.takeQuizViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 16);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(17).put(AiQuizViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (aiQuizViewModelProvider))).put(AuthViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (authViewModelProvider))).put(CreateQuizViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (createQuizViewModelProvider))).put(DashboardViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (dashboardViewModelProvider))).put(LeaderboardViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (leaderboardViewModelProvider))).put(MasterPaperViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (masterPaperViewModelProvider))).put(MoreViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (moreViewModelProvider))).put(ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (profileViewModelProvider))).put(QuestionBankViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (questionBankViewModelProvider))).put(QuizAnalysisViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (quizAnalysisViewModelProvider))).put(QuizDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (quizDetailViewModelProvider))).put(QuizDetailViewViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (quizDetailViewViewModelProvider))).put(QuizListViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (quizListViewModelProvider))).put(ResponseDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (responseDetailViewModelProvider))).put(ResponsesViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (responsesViewModelProvider))).put(SessionViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (sessionViewModelProvider))).put(TakeQuizViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (takeQuizViewModelProvider))).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.quizmaker.android.ui.aiquiz.AiQuizViewModel
          return (T) new AiQuizViewModel(singletonCImpl.authRepositoryProvider.get(), singletonCImpl.aiQuizRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 1: // com.quizmaker.android.ui.auth.AuthViewModel
          return (T) new AuthViewModel(singletonCImpl.authRepositoryProvider.get());

          case 2: // com.quizmaker.android.ui.quizcreate.CreateQuizViewModel
          return (T) new CreateQuizViewModel(singletonCImpl.authRepositoryProvider.get(), singletonCImpl.questionRepositoryProvider.get(), singletonCImpl.quizRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 3: // com.quizmaker.android.ui.dashboard.DashboardViewModel
          return (T) new DashboardViewModel(singletonCImpl.authRepositoryProvider.get(), singletonCImpl.quizRepositoryProvider.get(), singletonCImpl.questionRepositoryProvider.get());

          case 4: // com.quizmaker.android.ui.leaderboard.LeaderboardViewModel
          return (T) new LeaderboardViewModel(singletonCImpl.leaderboardRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 5: // com.quizmaker.android.ui.masterpaper.MasterPaperViewModel
          return (T) new MasterPaperViewModel(singletonCImpl.quizRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 6: // com.quizmaker.android.ui.more.MoreViewModel
          return (T) new MoreViewModel(singletonCImpl.authRepositoryProvider.get());

          case 7: // com.quizmaker.android.ui.profile.ProfileViewModel
          return (T) new ProfileViewModel(singletonCImpl.authRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get());

          case 8: // com.quizmaker.android.ui.questionbank.QuestionBankViewModel
          return (T) new QuestionBankViewModel(singletonCImpl.authRepositoryProvider.get(), singletonCImpl.questionRepositoryProvider.get());

          case 9: // com.quizmaker.android.ui.quizanalysis.QuizAnalysisViewModel
          return (T) new QuizAnalysisViewModel(singletonCImpl.quizAnalysisRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 10: // com.quizmaker.android.ui.quizdetail.QuizDetailViewModel
          return (T) new QuizDetailViewModel(singletonCImpl.quizRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 11: // com.quizmaker.android.ui.quizdetailview.QuizDetailViewViewModel
          return (T) new QuizDetailViewViewModel(singletonCImpl.quizDetailViewRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 12: // com.quizmaker.android.ui.quizlist.QuizListViewModel
          return (T) new QuizListViewModel(singletonCImpl.authRepositoryProvider.get(), singletonCImpl.quizRepositoryProvider.get());

          case 13: // com.quizmaker.android.ui.responsedetail.ResponseDetailViewModel
          return (T) new ResponseDetailViewModel(singletonCImpl.responseDetailRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 14: // com.quizmaker.android.ui.responses.ResponsesViewModel
          return (T) new ResponsesViewModel(singletonCImpl.authRepositoryProvider.get(), singletonCImpl.quizRepositoryProvider.get());

          case 15: // com.quizmaker.android.core.navigation.SessionViewModel
          return (T) new SessionViewModel(singletonCImpl.authRepositoryProvider.get());

          case 16: // com.quizmaker.android.ui.takequiz.TakeQuizViewModel
          return (T) new TakeQuizViewModel(singletonCImpl.quizTakingRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends QuizMakerApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends QuizMakerApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends QuizMakerApp_HiltComponents.SingletonC {
    private final SingletonCImpl singletonCImpl = this;

    Provider<SupabaseClient> provideSupabaseClientProvider;

    Provider<AuthRepository> authRepositoryProvider;

    Provider<QuestionRepository> questionRepositoryProvider;

    Provider<AiQuizRepository> aiQuizRepositoryProvider;

    Provider<QuizRepository> quizRepositoryProvider;

    Provider<LeaderboardRepository> leaderboardRepositoryProvider;

    Provider<ProfileRepository> profileRepositoryProvider;

    Provider<QuizAnalysisRepository> quizAnalysisRepositoryProvider;

    Provider<QuizDetailViewRepository> quizDetailViewRepositoryProvider;

    Provider<ResponseDetailRepository> responseDetailRepositoryProvider;

    Provider<QuizTakingRepository> quizTakingRepositoryProvider;

    SingletonCImpl() {

      initialize();

    }

    @SuppressWarnings("unchecked")
    private void initialize() {
      this.provideSupabaseClientProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseClient>(singletonCImpl, 1));
      this.authRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepository>(singletonCImpl, 0));
      this.questionRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<QuestionRepository>(singletonCImpl, 3));
      this.aiQuizRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AiQuizRepository>(singletonCImpl, 2));
      this.quizRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<QuizRepository>(singletonCImpl, 4));
      this.leaderboardRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<LeaderboardRepository>(singletonCImpl, 5));
      this.profileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ProfileRepository>(singletonCImpl, 6));
      this.quizAnalysisRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<QuizAnalysisRepository>(singletonCImpl, 7));
      this.quizDetailViewRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<QuizDetailViewRepository>(singletonCImpl, 8));
      this.responseDetailRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ResponseDetailRepository>(singletonCImpl, 9));
      this.quizTakingRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<QuizTakingRepository>(singletonCImpl, 10));
    }

    @Override
    public void injectQuizMakerApp(QuizMakerApp quizMakerApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.quizmaker.android.repository.AuthRepository
          return (T) new AuthRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 1: // io.github.jan.supabase.SupabaseClient
          return (T) SupabaseModule_ProvideSupabaseClientFactory.provideSupabaseClient();

          case 2: // com.quizmaker.android.repository.AiQuizRepository
          return (T) new AiQuizRepository(singletonCImpl.provideSupabaseClientProvider.get(), singletonCImpl.questionRepositoryProvider.get());

          case 3: // com.quizmaker.android.repository.QuestionRepository
          return (T) new QuestionRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 4: // com.quizmaker.android.repository.QuizRepository
          return (T) new QuizRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 5: // com.quizmaker.android.repository.LeaderboardRepository
          return (T) new LeaderboardRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 6: // com.quizmaker.android.repository.ProfileRepository
          return (T) new ProfileRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 7: // com.quizmaker.android.repository.QuizAnalysisRepository
          return (T) new QuizAnalysisRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 8: // com.quizmaker.android.repository.QuizDetailViewRepository
          return (T) new QuizDetailViewRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 9: // com.quizmaker.android.repository.ResponseDetailRepository
          return (T) new ResponseDetailRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 10: // com.quizmaker.android.repository.QuizTakingRepository
          return (T) new QuizTakingRepository(singletonCImpl.provideSupabaseClientProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
