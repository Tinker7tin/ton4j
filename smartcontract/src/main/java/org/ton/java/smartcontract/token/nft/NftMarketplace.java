package org.ton.java.smartcontract.token.nft;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

import static java.util.Objects.isNull;

@Builder
@Getter
public class NftMarketplace implements Contract {
  public static final String NFT_MARKETPLACE_CODE_HEX =
      "B5EE9C7241010401006D000114FF00F4A413F4BCF2C80B01020120020300AAD23221C700915BE0D0D3030171B0915BE0FA40ED44D0FA403012C705F2E19101D31F01C0018E2BFA003001D4D43021F90070C8CA07CBFFC9D077748018C8CB05CB0258CF165004FA0213CB6BCCCCC971FB00915BE20004F2308EF7CCE7";

  TweetNaclFast.Signature.KeyPair keyPair;
  Address adminAddress;

  public static class NftMarketplaceBuilder {}

  public static NftMarketplaceBuilder builder() {
    return new CustomNftMarketplaceBuilder();
  }

  private static class CustomNftMarketplaceBuilder extends NftMarketplaceBuilder {
    @Override
    public NftMarketplace build() {
      if (isNull(super.keyPair)) {
        super.keyPair = Utils.generateSignatureKeyPair();
      }
      if (isNull(super.adminAddress)) {
        throw new IllegalArgumentException("adminAddress parameter is mandatory.");
      }
      return super.build();
    }
  }

  private Tonlib tonlib;
  private long wc;

  @Override
  public Tonlib getTonlib() {
    return tonlib;
  }

  @Override
  public void setTonlib(Tonlib pTonlib) {
    tonlib = pTonlib;
  }

  @Override
  public long getWorkchain() {
    return wc;
  }

  public String getName() {
    return "nftMarketplace";
  }

  /**
   * @return Cell cell contains nft marketplace data
   */
  @Override
  public Cell createDataCell() {
    return CellBuilder.beginCell().storeAddress(adminAddress).endCell();
  }

  @Override
  public Cell createCodeCell() {
    return CellBuilder.beginCell().fromBoc(NFT_MARKETPLACE_CODE_HEX).endCell();
  }
}
