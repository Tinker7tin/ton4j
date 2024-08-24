package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV2R2Config;
import org.ton.java.smartcontract.wallet.v2.WalletV2R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.QueryFees;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletFeesV2 extends CommonTest {

    @Test
    public void testWalletFeesV2() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();

        TweetNaclFast.Signature.KeyPair keyPairA = Utils.generateSignatureKeyPair();

        WalletV2R2 walletA = WalletV2R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPairA)
                .build();

        String nonBounceableAddrWalletA = walletA.getAddress().toNonBounceable();
        String rawAddrWalletA = walletA.getAddress().toRaw();

        log.info("rawAddressA: {}", rawAddrWalletA);
        log.info("pub-key {}", Utils.bytesToHex(walletA.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(walletA.getKeyPair().getSecretKey()));

        TweetNaclFast.Signature.KeyPair keyPairB = Utils.generateSignatureKeyPair();

        WalletV2R2 walletB = WalletV2R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPairB)
                .build();

        String nonBounceableAddrWalletB = walletB.getAddress().toNonBounceable();
        String rawAddrWalletB = walletB.getAddress().toRaw();

        log.info("rawAddressB: {}", rawAddrWalletB);

        log.info("pub-key {}", Utils.bytesToHex(walletB.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(walletB.getKeyPair().getSecretKey()));

        // top up new walletA using test-faucet-wallet
        BigInteger balance1 = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddrWalletA), Utils.toNano(1));
        log.info("balance walletA: {}", Utils.formatNanoValue(balance1));

        // top up new walletB using test-faucet-wallet
        BigInteger balance2 = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddrWalletB), Utils.toNano(1));
        log.info("balance walletB: {} ", Utils.formatNanoValue(balance2));

        ExtMessageInfo extMessageInfo = walletA.deploy();
        assertThat(extMessageInfo.getError().getCode()).isZero();

        walletA.waitForDeployment(30);

        extMessageInfo = walletB.deploy();
        AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();

        walletB.waitForDeployment(30);

        // transfer 0.1 from walletA to walletB where B receives exact amount i.e. 0.1
        BigInteger balanceAbefore = walletA.getBalance();
        BigInteger balanceBbefore = walletB.getBalance();
        log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
        log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));

        WalletV2R2Config configA = WalletV2R2Config.builder()
                .seqno(walletA.getSeqno())
                .destination1(walletB.getAddress())
                .mode(3)
                .build();

        Message msg = walletA.prepareExternalMsg(configA);

        QueryFees fees = tonlib.estimateFees(
                walletB.getAddress().toBounceable(),
                msg.getBody().toBase64());

        // adjust amount by including storage fee
        configA.setAmount1(Utils.toNano(0.1).add(walletA.getGasFees()).add(BigInteger.valueOf(fees.getSource_fees().getStorage_fee())));

        log.info("fees on walletB with msg body from A: {}", fees);
        log.info("sending {}", Utils.formatNanoValue(configA.getAmount1()));

        walletA.send(configA);

        walletB.waitForBalanceChange(30);

        BigInteger balanceAafter = walletA.getBalance();
        BigInteger balanceBafter = walletB.getBalance();
        log.info("walletA balance after: {}", Utils.formatNanoValue(balanceAafter));
        log.info("walletB balance after: {}", Utils.formatNanoValue(balanceBafter));

        log.info("diff walletA (debited): -{}", Utils.formatNanoValue(balanceAbefore.subtract(balanceAafter)));
        log.info("diff walletB (credited): +{}, missing value {}", Utils.formatNanoValue(balanceBafter.subtract(balanceBbefore)),
                Utils.formatNanoValue(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore))));

        assertThat(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore))).isEqualTo(BigInteger.ZERO);
    }

    @Test
    public void testWithDeployedWalletsV2AB() {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();

        TweetNaclFast.Box.KeyPair keyPairBoxA = Utils.generateKeyPairFromSecretKey(Utils.hexToSignedBytes("49c61704e3e229e71c03b6729185e16ff1d5c23f521fcb9c61f49f4a9d02a5aaba54bea10e0125b24747ab5d849f0bff99b32c4e59fb7b176dc7556fdb52b0c3"));
        TweetNaclFast.Signature.KeyPair keyPairSignatureA = Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
        WalletV2R2 walletA = WalletV2R2.builder()
                .keyPair(keyPairSignatureA)
                .tonlib(tonlib)
                .build();
        log.info("rawAddressA {}", walletA.getAddress().toRaw());
        log.info("bounceableA {}", walletA.getAddress().toBounceable());

        TweetNaclFast.Box.KeyPair keyPairBoxB = Utils.generateKeyPairFromSecretKey(Utils.hexToSignedBytes("3c11edde736a9bbc576bab50650b1193439c35d2c206c5b1457828a22a8403a578fb62e179be94779082747bcc18044aa264329e6f53d4562e057f9a8856dfbc"));
        TweetNaclFast.Signature.KeyPair keyPairSignatureB = Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
        WalletV2R2 walletB = WalletV2R2.builder()
                .keyPair(keyPairSignatureB)
                .tonlib(tonlib)
                .build();
        log.info("rawAddressB {}", walletB.getAddress().toRaw());
        log.info("bounceableB {}", walletB.getAddress().toBounceable());


        BigInteger balanceAbefore = walletA.getBalance();
        BigInteger balanceBbefore = walletB.getBalance();
        log.info("walletA balance before: {}", Utils.formatNanoValue(balanceAbefore));
        log.info("walletB balance before: {}", Utils.formatNanoValue(balanceBbefore));


        WalletV2R2Config configA = WalletV2R2Config.builder()
                .seqno(walletA.getSeqno())
                .destination1(walletB.getAddress())
                .mode(3)
                .build();

        Message msg = walletA.prepareExternalMsg(configA);
        QueryFees feesWithCodeData = tonlib.estimateFees(
                walletB.getAddress().toBounceable(),
                msg.getBody().toBase64());

        //adjust new amount
        configA.setAmount1(Utils.toNano(0.1).add(walletA.getGasFees()).add(BigInteger.valueOf(feesWithCodeData.getSource_fees().getStorage_fee())));

        log.info("fees on walletB with msg body from A: {}", feesWithCodeData);

        walletA.send(configA);

        walletB.waitForBalanceChange(30);

        BigInteger balanceAafter = walletA.getBalance();
        BigInteger balanceBafter = walletB.getBalance();
        log.info("walletA balance after: {}", Utils.formatNanoValue(balanceAafter));
        log.info("walletB balance after: {}", Utils.formatNanoValue(balanceBafter));

        log.info("diff walletA (debited): -{}", Utils.formatNanoValue(balanceAbefore.subtract(balanceAafter)));
        log.info("diff walletB (credited): +{}, missing value {}", Utils.formatNanoValue(balanceBafter.subtract(balanceBbefore)),
                Utils.formatNanoValue(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore))));

        assertThat(Utils.toNano(0.1).subtract(balanceBafter.subtract(balanceBbefore))).isEqualTo(BigInteger.ZERO);
    }

    @Test
    public void testWalletStorageFeeSpeedV2() {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .build();

        TweetNaclFast.Box.KeyPair keyPairBoxA = Utils.generateKeyPairFromSecretKey(Utils.hexToSignedBytes("49c61704e3e229e71c03b6729185e16ff1d5c23f521fcb9c61f49f4a9d02a5aaba54bea10e0125b24747ab5d849f0bff99b32c4e59fb7b176dc7556fdb52b0c3"));
        TweetNaclFast.Signature.KeyPair keyPairSignatureA = Utils.generateSignatureKeyPairFromSeed(keyPairBoxA.getSecretKey());
        WalletV2R2 walletA = WalletV2R2.builder()
                .keyPair(keyPairSignatureA)
                .tonlib(tonlib)
                .build();
        log.info("rawAddressA {}", walletA.getAddress().toRaw());
        log.info("bounceableA {}", walletA.getAddress().toBounceable());

        TweetNaclFast.Box.KeyPair keyPairBoxB = Utils.generateKeyPairFromSecretKey(Utils.hexToSignedBytes("3c11edde736a9bbc576bab50650b1193439c35d2c206c5b1457828a22a8403a578fb62e179be94779082747bcc18044aa264329e6f53d4562e057f9a8856dfbc"));
        TweetNaclFast.Signature.KeyPair keyPairSignatureB = Utils.generateSignatureKeyPairFromSeed(keyPairBoxB.getSecretKey());
        WalletV2R2 walletB = WalletV2R2.builder()
                .keyPair(keyPairSignatureB)
                .tonlib(tonlib)
                .build();

        WalletV2R2Config configA = WalletV2R2Config.builder()
                .seqno(walletA.getSeqno())
                .destination1(walletB.getAddress())
                .mode(3)
                .build();

        Message msg = walletA.prepareExternalMsg(configA);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            QueryFees f = tonlib.estimateFees(
                    walletB.getAddress().toBounceable(),
                    msg.getBody().toBase64(), null, null, true);
            log.info("fees {}", f);
        }, 0, 15, TimeUnit.SECONDS);

        Utils.sleep(600);
    }
}
