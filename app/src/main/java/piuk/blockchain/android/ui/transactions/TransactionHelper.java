package piuk.blockchain.android.ui.transactions;

import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import info.blockchain.api.data.Input;
import info.blockchain.api.data.Transaction;
import info.blockchain.api.data.Transaction.Direction;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.HDWallet;
import info.blockchain.wallet.payload.data.Wallet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

public class TransactionHelper {

    private PayloadManager payloadManager;

    public TransactionHelper(PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
    }

    /**
     * Searches the list of Accounts to find if the address is labeled and returns that label
     *
     * @param address A bitcoin address
     * @return Either the label associated with the address, or the original address
     */
    @NonNull
    public String addressToLabel(String address) {

        HDWallet hdWallet = payloadManager.getPayload().getHdWallets().get(0);
        List<Account> accountList = new ArrayList<>();
        if (hdWallet != null && hdWallet.getAccounts() != null) {
            accountList = hdWallet.getAccounts();
        }

        // If address belongs to owned xpub
//        if (multiAddrFactory.isOwnHDAddress(address)) {
//            HashMap<String, String> addressToXpubMap = multiAddrFactory.getAddress2Xpub();
//            String xpub = addressToXpubMap.get(address);
//            if (xpub != null) {
//                // Even though it looks like this shouldn't happen, it sometimes happens with
//                // transfers if user clicks to view details immediately.
//                // TODO - see if isOwnHDAddress could be updated to solve this
//                // TODO: 21/02/2017
////                int accIndex = payloadManager.getXpubToAccountIndexMap().get(xpub);
////                String label = accountList.get(accIndex).getLabel();
////                if (label != null && !label.isEmpty())
////                    return label;
//            }
//            // If address one of owned legacy addresses
//        } else if (payloadManager.getPayload().getLegacyAddressStringList().contains(address)
//                || payloadManager.getPayload().getWatchOnlyAddressStringList().contains(address)) {
//
//            Wallet payload = payloadManager.getPayload();
//
//            String label = payload.getLegacyAddressList().get(payload.getLegacyAddressStringList().indexOf(address)).getLabel();
//            if (label != null && !label.isEmpty()) {
//                return label;
//            }
//        }

        return address;
    }

    /**
     * Return a Pair of maps that correspond to the inputs and outputs of a transaction, whilst
     * filtering out Change addresses.
     *
     * @param transactionDetails A {@link Transaction} object
     * @param transaction        An associated {@link Transaction}
     * @return A Pair of Maps representing the input and output of the transaction
     */
    @NonNull
    public Pair<HashMap<String, Long>, HashMap<String, Long>> filterNonChangeAddresses(Transaction transactionDetails, Transaction transaction) {

        throw new NotImplementedException("");
//        HashMap<String, String> addressToXpubMap = multiAddrFactory.getAddress2Xpub();
//
//        HashMap<String, Long> inputMap = new HashMap<>();
//        HashMap<String, Long> outputMap = new HashMap<>();
//
//        ArrayList<String> inputXpubList = new ArrayList<>();
//
//        // Inputs / From field
//        if (transaction.getDirection().equals(Direction.TRANSFERRED) && transactionDetails.getInputs().size() > 0) {
//            // Only 1 addr for receive
//            inputMap.put(transactionDetails.getInputs().get(0).addr, transactionDetails.getInputs().get(0).value);
//        } else {
//            for (Input input : transactionDetails.getInputs()) {
//                // Move or Send
//                // The address belongs to us
//                String xpub = addressToXpubMap.get(input.addr);
//
//                // Address belongs to xpub we own
//                if (xpub != null) {
//                    // Only add xpub once
//                    if (!inputXpubList.contains(xpub)) {
//                        inputMap.put(input.addr, input.value);
//                        inputXpubList.add(xpub);
//                    }
//                } else {
//                    // Legacy Address we own
//                    inputMap.put(input.addr, input.value);
//                }
//            }
//        }
//
//        // Outputs / To field
//        for (Transaction.xPut output : transactionDetails.getOutputs()) {
//
//            if (multiAddrFactory.isOwnHDAddress(output.addr)) {
//                // If output address belongs to an xpub we own - we have to check if it's change
//                String xpub = addressToXpubMap.get(output.addr);
//                if (inputXpubList.contains(xpub)) {
//                    continue;// change back to same xpub
//                }
//
//                // Receiving to same address multiple times?
//                if (outputMap.containsKey(output.addr)) {
//                    long prevAmount = outputMap.get(output.addr) + output.value;
//                    outputMap.put(output.addr, prevAmount);
//                } else {
//                    outputMap.put(output.addr, output.value);
//                }
//
//            } else if (payloadManager.getPayload().getLegacyAddressStringList().contains(output.addr)
//                    || payloadManager.getPayload().getWatchOnlyAddressStringList().contains(output.addr)) {
//                // If output address belongs to a legacy address we own - we have to check if it's change
//                // If it goes back to same address AND if it's not the total amount sent
//                // (inputs x and y could send to output y in which case y is not receiving change, but rather the total amount)
//                if (inputMap.containsKey(output.addr) && output.value != Math.abs(transaction.getAmount())) {
//                    continue;// change back to same input address
//                }
//
//                // Output more than tx amount - change
//                if (output.value > Math.abs(transaction.getAmount())) {
//                    continue;
//                }
//
//                outputMap.put(output.addr, output.value);
//            } else {
//                if (!transaction.getDirection().equals(MultiAddrFactory.RECEIVED)) {
//                    outputMap.put(output.addr, output.value);
//                }
//            }
//        }
//
//        return new Pair<>(inputMap, outputMap);
    }
}
