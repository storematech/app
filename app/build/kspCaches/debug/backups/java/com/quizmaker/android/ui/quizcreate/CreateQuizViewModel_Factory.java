package com.quizmaker.android.ui.quizcreate;

import androidx.lifecycle.SavedStateHandle;
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
public final class CreateQuizViewModel_Factory implements Factory<CreateQuizViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<QuestionRepository> questionRepositoryProvider;

  private final Provider<QuizRepository> quizRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public CreateQuizViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuestionRepository> questionRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.questionRepositoryProvider = questionRepositoryProvider;
    this.quizRepositoryProvider = quizRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public CreateQuizViewModel get() {
    return newInstance(authRepositoryProvider.get(), questionRepositoryProvider.get(), quizRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static CreateQuizViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuestionRepository> questionRepositoryProvider,
      Provider<QuizRepository> quizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new CreateQuizViewModel_Factory(authRepositoryProvider, questionRepositoryProvider, quizRepositoryProvider, savedStateHandleProvider);
  }

  public static CreateQuizViewModel newInstance(AuthRepository authRepository,
      QuestionRepository questionRepository, QuizRepository quizRepository,
      SavedStateHandle savedStateHandle) {
    return new CreateQuizViewModel(authRepository, questionRepository, quizRepository, savedStateHandle);
  }
}
