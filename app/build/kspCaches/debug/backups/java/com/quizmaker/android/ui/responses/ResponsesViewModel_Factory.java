package com.quizmaker.android.ui.responses;

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
public final class ResponsesViewModel_Factory implements Factory<ResponsesViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<QuizRepository> quizRepositoryProvider;

  public ResponsesViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.quizRepositoryProvider = quizRepositoryProvider;
  }

  @Override
  public ResponsesViewModel get() {
    return newInstance(authRepositoryProvider.get(), quizRepositoryProvider.get());
  }

  public static ResponsesViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider) {
    return new ResponsesViewModel_Factory(authRepositoryProvider, quizRepositoryProvider);
  }

  public static ResponsesViewModel newInstance(AuthRepository authRepository,
      QuizRepository quizRepository) {
    return new ResponsesViewModel(authRepository, quizRepository);
  }
}
