package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.NonNull;

import info.blockchain.api.data.MultiAddress;
import info.blockchain.api.data.Transaction;
import info.blockchain.wallet.payload.PayloadManager;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.BlockExplorerService;
import piuk.blockchain.android.data.stores.TransactionListStore;

public class TransactionListDataManager {

    private PayloadManager payloadManager;
    private BlockExplorerService blockExplorerService;
    private TransactionListStore transactionListStore;
    private Subject<List<Transaction>> listUpdateSubject;

    public TransactionListDataManager(PayloadManager payloadManager,
        BlockExplorerService blockExplorerService,
                                      TransactionListStore transactionListStore) {
        this.payloadManager = payloadManager;
        this.blockExplorerService = blockExplorerService;
        this.transactionListStore = transactionListStore;
        listUpdateSubject = PublishSubject.create();
    }

    public void generateTransactionList(String address) {
        transactionListStore.clearList();

        MultiAddress multiAddress = payloadManager.getMultiAddress(address);

        if(multiAddress != null) {
            transactionListStore
                .insertTransactions(multiAddress.getTxs());
        }
        listUpdateSubject.onNext(transactionListStore.getList());
        listUpdateSubject.onComplete();
    }

    /**
     * Returns a list of {@link Transaction} objects generated by {@link #getTransactionList()}
     *
     * @return A list of Txs sorted by date.
     */
    @NonNull
    public List<Transaction> getTransactionList() {
        return transactionListStore.getList();
    }

    /**
     * Resets the list of Transactions.
     */
    public void clearTransactionList() {
        transactionListStore.clearList();
    }

    /**
     * Allows insertion of a single new {@link Transaction} into the main transaction list.
     *
     * @param transaction A new, most likely temporary {@link Transaction}
     * @return An updated list of Txs sorted by date
     */
    @NonNull
    public List<Transaction> insertTransactionIntoListAndReturnSorted(Transaction transaction) {
        transactionListStore.insertTransactionIntoListAndSort(transaction);
        return transactionListStore.getList();
    }

    /**
     * Returns a subject that lets ViewModels subscribe to changes in the transaction list -
     * specifically this subject will return the transaction list when it's first updated and then
     * call onCompleted()
     *
     * @return The list of transactions after initial sync
     */
    public Subject<List<Transaction>> getListUpdateSubject() {
        return listUpdateSubject;
    }

    /**
     * Gets the final balance
     * @param address or xpub
     * @return
     */
    public double getBtcBalance(String address) {

        double balance = 0.0;

        MultiAddress multiAddress = payloadManager
            .getMultiAddress(address);

        if(multiAddress != null) {
            balance = multiAddress.getWallet().getFinalBalance().doubleValue();
        }

        return balance;
    }

    /**
     * Get a specific {@link Transaction} from a {@link Transaction} hash.
     *
     * @param transactionHash The hash of the transaction to be returned
     * @return A Transaction object
     */
    public Observable<Transaction> getTransactionFromHash(String transactionHash) {
        return blockExplorerService.getTransactionDetailsFromHash(transactionHash);
    }

    /**
     * Get a specific {@link Transaction} from a hash
     *
     * @param transactionHash The hash of the Tx to be returned
     * @return An Observable object wrapping a Tx. Will call onError if not found with a
     * NullPointerException
     */
    public Observable<Transaction> getTxFromHash(String transactionHash) {
        return Observable.create(emitter -> {
            //noinspection Convert2streamapi
            for (Transaction tx : getTransactionList()) {
                if (tx.getHash().equals(transactionHash)) {
                    if (!emitter.isDisposed()) {
                        emitter.onNext(tx);
                        emitter.onComplete();
                    }
                    return;
                }
            }

            if (!emitter.isDisposed()) emitter.onError(new NullPointerException("Tx not found"));
        });
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes           Transaction notes
     * @return If save was successful
     */
    public Observable<Boolean> updateTransactionNotes(String transactionHash, String notes) {
        payloadManager.getPayload().getTxNotes().put(transactionHash, notes);
        return Observable.fromCallable(() -> payloadManager.save())
                .compose(RxUtil.applySchedulersToObservable());
    }
}
