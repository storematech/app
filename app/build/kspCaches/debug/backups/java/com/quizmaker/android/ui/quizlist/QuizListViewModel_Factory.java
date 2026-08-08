package com.quizmaker.android.ui.quizlist;

import com.quizmaker.android.repository.AuthRepository;
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
public final class QuizListViewModel_Factory implements Factory<QuizListViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<QuizRepository> quizRepositoryProvider;

  public QuizListViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.quizRepositoryProvider = quizRepositoryProvider;
  }

  @Override
  public QuizListViewModel get() {
    return newInstance(authRepositoryProvider.get(), quizRepositoryProvider.get());
  }

  public static QuizListViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider) {
    return new QuizListViewModel_Factory(authRepositoryProvider, quizRepositoryProvider);
  }

  public static QuizListViewModel newInstance(AuthRepository authRepository,
      QuizRepository quizRepository) {
    return new QuizListViewModel(authRepository, quizRepository);
  }
}
