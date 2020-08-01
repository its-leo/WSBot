/*

import pl.zankowski.iextrading4j.api.marketdata.HIST;
import pl.zankowski.iextrading4j.api.stocks.ChartRange;
import pl.zankowski.iextrading4j.api.stocks.Quote;
import pl.zankowski.iextrading4j.api.stocks.v1.BatchStocks;
import pl.zankowski.iextrading4j.api.stocks.v1.TechnicalIndicator;
import pl.zankowski.iextrading4j.api.stocks.v1.TechnicalIndicatorType;
import pl.zankowski.iextrading4j.client.*;
import pl.zankowski.iextrading4j.client.rest.request.datapoint.TimeSeriesRequestBuilder;
import pl.zankowski.iextrading4j.client.rest.request.marketdata.HistRequestBuilder;
import pl.zankowski.iextrading4j.client.rest.request.stocks.QuoteRequestBuilder;

import pl.zankowski.iextrading4j.api.marketdata.TOPS;
import pl.zankowski.iextrading4j.client.IEXCloudClient;
import pl.zankowski.iextrading4j.client.IEXCloudTokenBuilder;
import pl.zankowski.iextrading4j.client.IEXTradingApiVersion;
import pl.zankowski.iextrading4j.client.IEXTradingClient;
import pl.zankowski.iextrading4j.client.rest.request.stocks.v1.BatchStocksRequestBuilder;
import pl.zankowski.iextrading4j.client.rest.request.stocks.v1.BatchStocksType;
import pl.zankowski.iextrading4j.client.rest.request.stocks.v1.TechnicalIndicatorRequestBuilder;
import pl.zankowski.iextrading4j.client.sse.manager.SseRequest;
import pl.zankowski.iextrading4j.client.sse.request.marketdata.TopsSseRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Main2 {

    public static void main(String[] args) {


        /*
        final IEXCloudClient cloudClient = IEXTradingClient.create(new IEXCloudTokenBuilder()
                        .withPublishableToken("Tpk_18dfe6cebb4f41ffb219b9680f9acaf2")
                        .withSecretToken("Tsk_3eedff6f5c284e1a8b9bc16c54dd1af3")
                        .build());

        final Consumer<List<TOPS>> TOPS_CONSUMER = System.out::println;
        final SseRequest<List<TOPS>> request = new TopsSseRequestBuilder()
                .withSymbol("AAPL")
                .build();

        cloudClient.subscribe(request, TOPS_CONSUMER);



        final IEXApiClient iexTradingClient = IEXTradingClient.create();
        final List<HIST> histList = iexTradingClient.executeRequest(new HistRequestBuilder()
                .withDate(LocalDate.of(2020, 5, 15))
                .build());
        System.out.println(histList);


    }
}*/
