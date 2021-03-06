/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.client.internal.bus;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.eclipse.ditto.protocoladapter.Adaptable;

/**
 * Event bus for messages that are either {@code String} or {@code} Adaptable.
 * On publication of a message as {@code String}, subscribers are notified as follows:
 * <ol>
 * <li>Message is classified as {@code String}. If a matching one-time subscriber is found, the subscriber is notified
 * and removed.</li>
 * <li>Message is classified as {@code Adaptable}. If a matching one-time subscriber is found, the subscriber is
 * notified and removed.</li>
 * <li>Message is classified as {@code Adaptable}. If a persistent subscriber is found, the subscriber is notified.
 * </li>
 * <li>The unhandled subscriber is notified.</li>
 * </ol>
 */
public interface AdaptableBus {

    /**
     * Add another string classifier.
     *
     * @param classifier the string classifier.
     * @return this object.
     */
    AdaptableBus addStringClassifier(Classifier<String> classifier);

    /**
     * Add another adaptable classifier.
     *
     * @param adaptableClassifier the adaptable classifier
     * @return this object.
     */
    AdaptableBus addAdaptableClassifier(Classifier<Adaptable> adaptableClassifier);

    /**
     * Add a one-time subscriber for a string message.
     *
     * @param tag the string classification, usually itself.
     * @param timeout how long to wait for a match.
     * @return a future adaptable matching the tag according to the classifiers, or a failed future
     * if no adaptable is matched within the timeout.
     */
    CompletionStage<String> subscribeOnceForString(Classification tag, Duration timeout);

    /**
     * Add a one-time subscriber for an adaptable message. Only effective if no one-time string subscriber matches.
     *
     * @param tag the adaptable classification.
     * @param timeout how long to wait for a match.
     * @return a future adaptable matching the tag according to the classifiers, or a failed future
     * if no adaptable is matched within the timeout.
     */
    CompletionStage<Adaptable> subscribeOnceForAdaptable(Classification tag, Duration timeout);

    /**
     * Add a persistent subscriber for an adaptable message. Only effective if no one-time string or adaptable
     * subscriber matches.
     * If tag requires sequentialization, take care that the consumer is fast, or the bus will block.
     *
     * @param tag the adaptable classification.
     * @param adaptableConsumer the consumer of the adaptable message.
     * @return the subscription ID.
     */
    SubscriptionId subscribeForAdaptable(Classification tag, Consumer<Adaptable> adaptableConsumer);

    /**
     * Add a persistent subscriber for an adaptable message and remove all other subscribers.
     * Only effective if no one-time string or adaptable subscriber matches.
     *
     * @param tag the adaptable classification.
     * @param adaptableConsumer the consumer of the adaptable message.
     * @return the subscription ID.
     */
    SubscriptionId subscribeForAdaptableExclusively(Classification tag, Consumer<Adaptable> adaptableConsumer);

    /**
     * Add a persistent subscriber for an adaptable message that are removed after a timeout.
     * If tag requires sequentialization, take care that all consumer and predicate parameters are fast,
     * or the bus will block.
     *
     * @param tag the adaptable classification.
     * @param timeout how long to wait to remove the subscriber after matching messages stopped arriving.
     * @param adaptableConsumer consumer of non-termination messages.
     * @param terminationPredicate predicate for termination messages.
     * @param onTimeout what to do in case of timeout.
     * @return the subscription ID.
     */
    SubscriptionId subscribeForAdaptableWithTimeout(Classification tag,
            Duration timeout,
            Consumer<Adaptable> adaptableConsumer,
            Predicate<Adaptable> terminationPredicate,
            final Consumer<Throwable> onTimeout);

    /**
     * Remove a subscription from the bus. Do nothing if the subscription ID is null.
     *
     * @param subscriptionId the subscription ID.
     * @return whether the removed subscription exists.
     */
    boolean unsubscribe(@Nullable SubscriptionId subscriptionId);

    /**
     * @return the scheduled executor service of this adaptable bus.
     */
    ScheduledExecutorService getScheduledExecutor();

    /**
     * Closes the executor of the adaptable bus .
     */
    void shutdownExecutor();

    /**
     * Publish a string message that may or may not be an adaptable.
     *
     * @param message the string message.
     */
    void publish(String message);

    /**
     * An empty interface to mark adaptable bus subscriptions.
     */
    interface SubscriptionId {}
}
