package com.quizmaker.android.ui.dashboard;

import com.quizmaker.android.repository.AuthRepository;
import com.quizmaker.android.repository.QuestionRepository;
import com.quizmaker.android.repository.QuizRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<QuizRepository> quizRepositoryProvider;

  private final Provider<QuestionRepository> questionRepositoryProvider;

  public DashboardViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider,
      Provider<QuestionRepository> questionRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.quizRepositoryProvider = quizRepositoryProvider;
    this.questionRepositoryProvider = questionRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(authRepositoryProvider.get(), quizRepositoryProvider.get(), questionRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider,
      Provider<QuestionRepository> questionRepositoryProvider) {
    return new DashboardViewModel_Factory(authRepositoryProvider, quizRepositoryProvider, questionRepositoryProvider);
  }

  public static DashboardViewModel newInstance(AuthRepository authRepository,
      QuizRepository quizRepository, QuestionRepository questionRepository) {
    return new DashboardViewModel(authRepository, quizRepository, questionRepository);
  }
}
