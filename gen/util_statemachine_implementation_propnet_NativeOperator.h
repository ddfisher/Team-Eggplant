/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class util_statemachine_implementation_propnet_NativeOperator */

#ifndef _Included_util_statemachine_implementation_propnet_NativeOperator
#define _Included_util_statemachine_implementation_propnet_NativeOperator
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    transition
 * Signature: ([Z)V
 */
JNIEXPORT void JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_transition
  (JNIEnv *, jobject, jbooleanArray);

/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    propagateInternal
 * Signature: ([Z)V
 */
JNIEXPORT void JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_propagateInternal
  (JNIEnv *, jobject, jbooleanArray);

/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    propagate
 * Signature: ([Z)V
 */
JNIEXPORT void JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_propagate
  (JNIEnv *, jobject, jbooleanArray);

/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    propagateTerminalOnly
 * Signature: ([Z)V
 */
JNIEXPORT void JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_propagateTerminalOnly
  (JNIEnv *, jobject, jbooleanArray);

/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    propagateLegalOnly
 * Signature: ([ZII)V
 */
JNIEXPORT void JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_propagateLegalOnly
  (JNIEnv *, jobject, jbooleanArray, jint, jint);

/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    propagateGoalOnly
 * Signature: ([ZI)V
 */
JNIEXPORT void JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_propagateGoalOnly
  (JNIEnv *, jobject, jbooleanArray, jint);

/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    monteCarlo
 * Signature: ([Z)I
 */
JNIEXPORT jint JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_monteCarlo
  (JNIEnv *, jobject, jbooleanArray);

/*
 * Class:     util_statemachine_implementation_propnet_NativeOperator
 * Method:    initMonteCarlo
 * Signature: ([[I[I[I[I)V
 */
JNIEXPORT void JNICALL Java_util_statemachine_implementation_propnet_NativeOperator_initMonteCarlo
  (JNIEnv *, jobject, jobjectArray, jintArray, jintArray, jintArray);

#ifdef __cplusplus
}
#endif
#endif
