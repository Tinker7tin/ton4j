package org.ton.java.smartcontract.wallet.v1;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.types.WalletCodes;
import org.ton.java.smartcontract.types.WalletV1R1Config;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.tlb.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

public class WalletV1ContractR1 implements Contract<WalletV1R1Config> {

    Options options;

    /**
     * @param options Options
     */
    public WalletV1ContractR1(Options options) {
        this.options = options;
        options.code = CellBuilder.beginCell().fromBoc(WalletCodes.V1R1.getValue()).endCell();
    }

    @Override
    public String getName() {
        return "V1R1";
    }

    @Override
    public Cell createDataCell() {
        CellBuilder cell = CellBuilder.beginCell();
        cell.storeUint(BigInteger.ZERO, 32); // seqno
        cell.storeBytes(getOptions().publicKey);
        return cell.endCell();
    }

    public Cell createDeployMessage() {
        return CellBuilder.beginCell().storeUint(BigInteger.ZERO, 32).endCell();
    }

    public Cell createTransferBody(WalletV1R1Config config) {
        Cell order = Message.builder()
                .info(InternalMessageInfo.builder()
                        .dstAddr(MsgAddressIntStd.builder()
                                .workchainId(config.getDestination().wc)
                                .address(config.getDestination().toBigInteger())
                                .build())
                        .value(CurrencyCollection.builder().coins(config.getAmount()).build())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeUint(0, 32)
                        .storeString(config.getComment())
                        .endCell())
                .build().toCell();

        return CellBuilder.beginCell()
                .storeUint(BigInteger.valueOf(config.getSeqno()), 32)
                .storeUint(config.getMode() & 0xff, 8)
                .storeRef(order)
                .endCell();
    }

    @Override
    public Options getOptions() {
        return options;
    }

    /**
     * Sends amount of nano toncoins to destination address using specified seqno
     *
     * @param tonlib Tonlib
     * @param config WalletV1R1Config
     */
    public ExtMessageInfo sendTonCoins(Tonlib tonlib, WalletV1R1Config config) {
        Cell body = createTransferBody(config);
        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }

    @Override
    public ExtMessageInfo deploy(Tonlib tonlib, WalletV1R1Config config) {
        Cell body = createDeployMessage();

        Message externalMessage = Message.builder()
                .info(ExternalMessageInfo.builder()
                        .dstAddr(getAddressIntStd())
                        .build())
                .init(createStateInit())
                .body(CellBuilder.beginCell()
                        .storeBytes(Utils.signData(getOptions().getPublicKey(), options.getSecretKey(), body.hash()))
                        .storeCell(body)
                        .endCell())
                .build();

        return tonlib.sendRawMessage(externalMessage.toCell().toBase64());
    }
}
