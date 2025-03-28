/*
 * This file is/was part of Treasury. To read more information about Treasury such as its licensing, see <https://github.com/lokka30/Treasury>.
 */

package me.lokka30.treasury.api.economy.response;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * An interface accepting responses from an economic provider.
 * Used to subscribe to a request that will be completed at
 * some point in the future.
 *
 * @param <T> the type of value expected on success
 * @author Jikoo
 * @since {@link me.lokka30.treasury.api.economy.misc.EconomyAPIVersion#v1_0 v1.0}
 */
public interface EconomySubscriber<T> {

    /**
     * Respond to the subscriber with a successful invocation.
     *
     * @param t the value of the successful invocation
     * @author Jikoo
     * @since {@link me.lokka30.treasury.api.economy.misc.EconomyAPIVersion#v1_0 v1.0}
     */
    void succeed(@NotNull T t);

    /**
     * Respond to the subscriber with an invocation failure.
     *
     * @param exception an {@link EconomyException} detailing the reason for failure
     * @author Jikoo
     * @since {@link me.lokka30.treasury.api.economy.misc.EconomyAPIVersion#v1_0 v1.0}
     */
    void fail(@NotNull EconomyException exception);

    /**
     * Wrap a method accepting an {@link EconomySubscriber} in a {@link CompletableFuture}.
     *
     * <p>This allows easy conversion from the more expressive style used by Treasury
     * into a quicker to use format. For example, setting a player's balance:
     * <pre>{@code
     *     public void setBalance(
     *             @NotNull CommandSender issuer,
     *             @NotNull UUID target,
     *             BigDecimal balance,
     *             @NotNull Currency currency) {
     *         EconomyProvider economy = // Obtain provider
     *
     *         // First we need to obtain the account.
     *         EconomySubscriber.asFuture(subscriber -> economy.retrievePlayerAccount(target, subscriber))
     *             // Then we set the balance.
     *             .thenCompose(account -> EconomySubscriber.asFuture(subscriber -> account.setBalance(balance, currency, subscriber)))
     *             // And then we can use the final value however we like.
     *             .whenComplete((newBalance, exception) -> {
     *                 if (exception != null) {
     *                     sender.sendMessage("Something went wrong!");
     *                 } else {
     *                     sender.sendMessage(String.format("Set balance to %s.", newBalance));
     *                 }
     *             });
     *     }
     * }</pre>
     * Note that due to the lack of explict requirement it is far easier to
     * forget exception handling.
     *
     * @param subscriberConsumer a {@link Consumer} accepting an {@code EconomySubscriber}
     * @param <T>                the type of value expected by the {@code EconomySubscriber}
     * @return a future awaiting subscriber completion
     * @author Jikoo
     * @since {@link me.lokka30.treasury.api.economy.misc.EconomyAPIVersion#v1_0 v1.0}
     */
    static <T> @NotNull CompletableFuture<T> asFuture(@NotNull Consumer<EconomySubscriber<T>> subscriberConsumer) {
        CompletableFuture<T> future = new CompletableFuture<>();
        EconomySubscriber<T> subscriber = new EconomySubscriber<T>() {
            @Override
            public void succeed(@NotNull T t) {
                future.complete(t);
            }

            @Override
            public void fail(@NotNull EconomyException exception) {
                future.completeExceptionally(exception);
            }
        };

        subscriberConsumer.accept(subscriber);

        return future;
    }

}
