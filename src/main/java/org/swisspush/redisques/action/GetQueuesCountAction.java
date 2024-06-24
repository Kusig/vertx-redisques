package org.swisspush.redisques.action;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.swisspush.redisques.exception.RedisQuesExceptionFactory;
import org.swisspush.redisques.handler.GetQueuesCountHandler;
import org.swisspush.redisques.util.*;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.swisspush.redisques.util.RedisquesAPI.*;

public class GetQueuesCountAction extends GetQueuesAction {

    private final RedisQuesExceptionFactory exceptionFactory;

    public GetQueuesCountAction(
            Vertx vertx, RedisProvider redisProvider, RedisQuesExceptionFactory exceptionFactory,
            String address, String queuesKey, String queuesPrefix, String consumersPrefix, String locksKey,
            List<QueueConfiguration> queueConfigurations, QueueStatisticsCollector queueStatisticsCollector,
            Logger log
    ) {
        super(vertx, redisProvider, address, queuesKey, queuesPrefix, consumersPrefix, locksKey,
                queueConfigurations, queueStatisticsCollector, log);
        this.exceptionFactory = exceptionFactory;
    }

    @Override
    public void execute(Message<JsonObject> event) {
        Result<Optional<Pattern>, String> result = MessageUtil.extractFilterPattern(event);
        if (result.isErr()) {
            event.reply(createErrorReply().put(ERROR_TYPE, BAD_INPUT).put(MESSAGE, result.getErr()));
            return;
        }

        /*
         * to filter values we have to use "getQueues" operation
         */
        if (result.getOk().isPresent()) {
            getQueues(event, true, result);
        } else {
            redisProvider.redis().onSuccess(redisAPI -> redisAPI.zcount(queuesKey, String.valueOf(getMaxAgeTimestamp()),
                            String.valueOf(Double.MAX_VALUE), new GetQueuesCountHandler(event, exceptionFactory)))
                    .onFailure(ex -> replyErrorMessageHandler(event).handle(ex));
        }
    }

}
