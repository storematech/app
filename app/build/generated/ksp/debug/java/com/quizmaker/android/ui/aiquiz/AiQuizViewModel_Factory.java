package com.quizmaker.android.ui.aiquiz;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.AiQuizRepository;
import com.quizmaker.android.repository.AuthRepository;
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
public final class AiQuizViewModel_Factory implements Factory<AiQuizViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<AiQuizRepository> aiQuizRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public AiQuizViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<AiQuizRepository> aiQuizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.aiQuizRepositoryProvider = aiQuizRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public AiQuizViewModel get() {
    return newInstance(authRepositoryProvider.get(), aiQuizRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static AiQuizViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<AiQuizRepository> aiQuizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new AiQuizViewModel_Factory(authRepositoryProvider, aiQuizRepositoryProvider, savedStateHandleProvider);
  }

  public static AiQuizViewModel newInstance(AuthRepository authRepository,
      AiQuizRepository aiQuizRepository, SavedStateHandle savedStateHandle) {
    return new AiQuizViewModel(authRepository, aiQuizRepository, savedStateHandle);
  }
}
