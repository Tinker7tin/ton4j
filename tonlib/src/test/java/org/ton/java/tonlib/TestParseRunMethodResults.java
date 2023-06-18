package org.ton.java.tonlib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.java.tonlib.TestTonlibJson.ELECTOR_ADDRESSS;

@Slf4j
@RunWith(JUnit4.class)
public class TestParseRunMethodResults {

    private static final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();

    @Test
    public void tesTvmRenderStack() {

        CellBuilder c1 = CellBuilder.beginCell();
        c1.storeUint((long) Math.pow(2, 25), 26);

        Deque<String> d = new ArrayDeque<>();

        d.offer("[num, 300]");
        d.offer("[cell, " + Utils.bytesToHex(c1.endCell().toBocNew()) + "]");

        Deque<TvmStackEntry> r0 = ParseRunResult.renderTvmStack(d);
        Deque<TvmStackEntry> r1 = ((ArrayDeque) r0).clone();
        log.info(r0.pop().toString());
        log.info(r0.pop().toString());
        assertThat(r0.isEmpty()).isTrue();

        log.info("size {}", r1.size());

        Deque<String> s = ParseRunResult.serializeTvmStack(r1);
        log.info(s.pop());
        log.info(s.pop());
        assertThat(r0.size()).isZero();
    }

    @Test
    public void testGetTypeRunResultStackTuple() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryTuple.json")));

        RunResult result = new RunResultParser().parse(stackTuple);
        log.info("result {}", result.toString());
    }

    @Test
    public void testGetTypeRunResultStackList() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryList.json")));

        RunResult result = new RunResultParser().parse(stackTuple);
        log.info("result {}", result.toString());
    }

    @Test
    public void testGetTypeRunResultStackListDeserializer() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodNumbers.json")));


        RunResult result = new RunResultParser().parse(stackTuple);
        log.info("RunResult: {}", result);

    }

    @Test
    public void testGetTypeRunResultStackTupleWithList() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryTupleWithList.json")));

        RunResult result = new RunResultParser().parse(stackTuple);
        log.info(result.toString());
    }

    @Test
    public void testGetTypeRunResultStackEntryListWithTuple() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultStackEntryListWithTuple.json")));

        RunResult result = new RunResultParser().parse(stackTuple);
        log.info(result.toString());
    }

    @Test
    public void testGetTypeRunResultStackEntryListPastElections() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodResultPastElections.json")));

        RunResult result = new RunResultParser().parse(stackTuple);
        log.info("result {}", result.toString());
    }

    @Test
    public void testGetTypeRunResultGetSubscriptionData() {
        String stackTuple = Utils.streamToString(Objects.requireNonNull(TestTonlibJson.class.getClassLoader().getResourceAsStream("RunMethodGetSubscriptionData.json")));

        RunResult result = new RunResultParser().parse(stackTuple);
        log.info("result {}", result.toString());

        TvmStackEntryTuple walletAddr = (TvmStackEntryTuple) result.getStack().get(0);
        TvmStackEntryNumber wc = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber hash = (TvmStackEntryNumber) walletAddr.getTuple().getElements().get(1);
        log.info("walletAddr: {}:{}", wc.getNumber(), hash.getNumber().toString(16));

        TvmStackEntryTuple beneficiaryAddr = (TvmStackEntryTuple) result.getStack().get(1);
        TvmStackEntryNumber beneficiaryAddrWc = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(0);
        TvmStackEntryNumber beneficiaryAddrHash = (TvmStackEntryNumber) beneficiaryAddr.getTuple().getElements().get(1);
        log.info("walletAddr: {}:{}", beneficiaryAddrWc.getNumber(), beneficiaryAddrHash.getNumber().toString(16));

        TvmStackEntryNumber amount = (TvmStackEntryNumber) result.getStack().get(2);
        log.info("amount: {}", amount.getNumber());

        TvmStackEntryNumber period = (TvmStackEntryNumber) result.getStack().get(3);
        log.info("period: {}", period.getNumber());

        TvmStackEntryNumber startTime = (TvmStackEntryNumber) result.getStack().get(4);
        log.info("startTime: {}", startTime.getNumber());

        TvmStackEntryNumber timeOut = (TvmStackEntryNumber) result.getStack().get(5);
        log.info("timeOut: {}", timeOut.getNumber());

        TvmStackEntryNumber lastPaymentTime = (TvmStackEntryNumber) result.getStack().get(6);
        log.info("lastPaymentTime: {}", lastPaymentTime.getNumber());

        TvmStackEntryNumber lastRequestTime = (TvmStackEntryNumber) result.getStack().get(7);
        log.info("lastRequestTime: {}", lastRequestTime.getNumber());

        TvmStackEntryNumber failedAttempts = (TvmStackEntryNumber) result.getStack().get(8);
        log.info("failedAttempts: {}", failedAttempts.getNumber());

        TvmStackEntryNumber subscriptionId = (TvmStackEntryNumber) result.getStack().get(9);
        log.info("subscriptionId: {}", subscriptionId.getNumber());
    }

    @Test
    public void testTonlibComputeReturnedStake() {
        Tonlib tonlib = Tonlib.builder()
                .build();

        MasterChainInfo last = tonlib.getLast();
        log.info("last: {}", last);

        Address elector = Address.of(ELECTOR_ADDRESSS);
        Deque<String> stack = new ArrayDeque<>();
        Address address = Address.of("Ef_sR2c8U-tNfCU5klvd60I5VMXUd_U9-22uERrxrrt3uzYi");
        stack.offer("[num, " + address.toDecimal() + "]");

        RunResult result = tonlib.runMethod(elector, "compute_returned_stake", stack);

        BigInteger returnStake = ((TvmStackEntryNumber) result.getStack().get(0)).getNumber();

        log.info("return stake: {} ", Utils.formatNanoValue(returnStake.longValue()));
        Assertions.assertThat(result.getExit_code()).isEqualTo(0L);
    }
}
