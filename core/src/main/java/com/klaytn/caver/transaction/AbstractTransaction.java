/*
 * Copyright 2020 The caver-java Authors
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klaytn.caver.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.klaytn.caver.rpc.Klay;
import com.klaytn.caver.account.AccountKeyRoleBased;
import com.klaytn.caver.transaction.type.TransactionType;
import com.klaytn.caver.utils.Utils;
import com.klaytn.caver.wallet.keyring.AbstractKeyring;
import com.klaytn.caver.wallet.keyring.KeyringFactory;
import com.klaytn.caver.wallet.keyring.SignatureData;
import org.web3j.crypto.Hash;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.*;
import java.util.function.Function;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
abstract public class AbstractTransaction {

    /**
     * Klay RPC instance
     */
    @JsonIgnore
    private Klay klaytnCall = null;

    /**
     * Transaction's type string
     */
    @JsonIgnore
    private String type;

    /**
     * The address of the sender.
     */
    private String from;

    /**
     * A value used to uniquely identify a sender’s transaction.
     * If two transactions with the same nonce are generated by a sender, only one is executed.
     */
    private String nonce = "0x";

    /**
     * The maximum amount of gas the transaction is allowed to use.
     */
    private String gas;

    /**
     * Network ID
     */
    private String chainId = "0x";

    /**
     * A Signature list
     */
    private List<SignatureData> signatures = new ArrayList<>();

    /**
     * Represents a AbstractTransaction class builder.
     * @param <B> An generic extends to AbstractTransaction.Builder
     */
    public static class Builder<B extends AbstractTransaction.Builder> {
        private String type;
        private String gas;

        private String from;
        private String nonce = "0x";
        private String chainId = "0x";
        private Klay klaytnCall = null;
        private List<SignatureData> signatures = new ArrayList<>();

        public Builder(String type) {
            this.type = type;
        }

        public B setFrom(String from) {
            this.from = from;
            return (B) this;
        }

        public B setNonce(String nonce) {
            this.nonce = nonce;
            return (B) this;
        }

        public B setNonce(BigInteger nonce) {
            setNonce(Numeric.toHexStringWithPrefix(nonce));
            return (B) this;
        }

        public B setGas(String gas) {
            this.gas = gas;
            return (B) this;
        }

        public B setGas(BigInteger gas) {
            setGas(Numeric.toHexStringWithPrefix(gas));
            return (B) this;
        }

        public B setChainId(String chainId) {
            this.chainId = chainId;
            return (B) this;
        }

        public B setChainId(BigInteger chainId) {
            setChainId(Numeric.toHexStringWithPrefix(chainId));
            return (B) this;
        }

        public B setKlaytnCall(Klay klaytnCall) {
            this.klaytnCall = klaytnCall;
            return (B) this;
        }

        public B setSignatures(List<SignatureData> signatures) {
            this.signatures.addAll(signatures);
            return (B) this;
        }

        public B setSignatures(SignatureData sign) {
            if(sign == null) {
                sign = SignatureData.getEmptySignature();
            }

            this.signatures.add(sign);
            return (B) this;
        }
    }

    /**
     * Create AbstractTransaction instance
     * @param builder AbstractTransaction.builder
     */
    public AbstractTransaction(AbstractTransaction.Builder builder) {
        this(builder.klaytnCall,
                builder.type,
                builder.from,
                builder.nonce,
                builder.gas,
                builder.chainId,
                builder.signatures
        );
    }

    /**
     * Create AbstractTransaction instance
     * @param klaytnCall Klay RPC instance
     * @param type Transaction's type string
     * @param from The address of the sender.
     * @param nonce A value used to uniquely identify a sender’s transaction.
     * @param gas The maximum amount of gas the transaction is allowed to use.
     * @param chainId Network ID
     * @param signatures A Signature list
     */
    public AbstractTransaction(Klay klaytnCall, String type, String from, String nonce, String gas, String chainId, List<SignatureData> signatures) {
        setKlaytnCall(klaytnCall);
        setType(type);
        setFrom(from);
        setNonce(nonce);
        setGas(gas);
        setChainId(chainId);
        setSignatures(signatures);
    }

    /**
     * Returns the RLP-encoded string of this transaction (i.e., rawTransaction).
     * @return String
     */
    @JsonIgnore
    public abstract String getRLPEncoding();

    /**
     * Returns the RLP-encoded string to make the signature of this transaction.
     * @return String
     */
    @JsonIgnore
    public abstract String getCommonRLPEncodingForSignature();

    /**
     * Signs to the transaction with a single private key.
     * It sets Hasher default value.
     *   - signer : TransactionHasher.getHashForSignature()
     * @param keyString The private key string.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction sign(String keyString) throws IOException {
        AbstractKeyring keyring = KeyringFactory.createFromPrivateKey(keyString);
        return this.sign(keyring, TransactionHasher::getHashForSignature);
    }

    /**
     * Signs to the transaction with a single private key.
     * @param keyString The private key string
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction sign(String keyString, Function<AbstractTransaction, String> signer) throws IOException {
        AbstractKeyring keyring = KeyringFactory.createFromPrivateKey(keyString);
        return this.sign(keyring, signer);
    }

    /**
     * Signs using all private keys used in the role defined in the Keyring instance.
     * It sets index and Hasher default value.
     *   - signer : TransactionHasher.getHashForSignature()
     * @param keyring The Keyring instance.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction sign(AbstractKeyring keyring) throws IOException  {
        return this.sign(keyring, TransactionHasher::getHashForSignature);
    }

    /**
     * Signs using all private keys used in the role defined in the Keyring instance.
     * @param keyring The Keyring instance.
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction sign(AbstractKeyring keyring, Function<AbstractTransaction, String> signer) throws IOException  {
        if(TransactionHelper.isEthereumTransaction(this.getType()) && keyring.isDecoupled()) {
            throw new IllegalArgumentException(this.getType() + " cannot be signed with a decoupled keyring.");
        }

        if(this.from.equals("0x") || this.from.equals(Utils.DEFAULT_ZERO_ADDRESS)){
            this.from = keyring.getAddress();
        }

        if(!this.from.toLowerCase().equals(keyring.getAddress().toLowerCase())) {
            throw new IllegalArgumentException("The from address of the transaction is different with the address of the keyring to use");
        }

        this.fillTransaction();
        int role = this.type.contains("AccountUpdate") ? AccountKeyRoleBased.RoleGroup.ACCOUNT_UPDATE.getIndex() : AccountKeyRoleBased.RoleGroup.TRANSACTION.getIndex();

        String hash = signer.apply(this);
        List<SignatureData> sigList = keyring.sign(hash, Numeric.toBigInt(this.chainId).intValue(), role);

        this.appendSignatures(sigList);

        return this;
    }

    /**
     * Signs to the transaction with a private key in the Keyring instance.
     * It sets signer to TransactionHasher.getHashForSignature()
     * @param keyring The Keyring instance.
     * @param index The index of private key to use in Keyring instance.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction sign(AbstractKeyring keyring, int index) throws IOException {
        return this.sign(keyring, index, TransactionHasher::getHashForSignature);
    }

    /**
     * Signs to the transaction with a private key in the Keyring instance.
     * @param keyring The Keyring instance.
     * @param index The index of private key to use in Keyring instance.
     * @param signer The function to get hash of transaction.
     * @return AbstractTransaction
     * @throws IOException
     */
    public AbstractTransaction sign(AbstractKeyring keyring, int index, Function<AbstractTransaction, String> signer) throws IOException {
        if(TransactionHelper.isEthereumTransaction(this.getType()) && keyring.isDecoupled()) {
            throw new IllegalArgumentException(this.getType() + " cannot be signed with a decoupled keyring.");
        }

        if(this.from.equals("0x") || this.from.equals(Utils.DEFAULT_ZERO_ADDRESS)){
            this.from = keyring.getAddress();
        }

        if(!this.from.toLowerCase().equals(keyring.getAddress().toLowerCase())) {
            throw new IllegalArgumentException("The from address of the transaction is different with the address of the keyring to use");
        }

        this.fillTransaction();
        int role = this.type.contains("AccountUpdate") ? AccountKeyRoleBased.RoleGroup.ACCOUNT_UPDATE.getIndex() : AccountKeyRoleBased.RoleGroup.TRANSACTION.getIndex();

        String hash = signer.apply(this);
        SignatureData sig = keyring.sign(hash, Numeric.toBigInt(this.chainId).intValue(), role, index);

        this.appendSignatures(sig);

        return this;
    }

    /**
     * Appends signatures to the transaction.
     * @param signatureData SignatureData instance contains ECDSA signature data
     */
    public void appendSignatures(SignatureData signatureData) {
        List<SignatureData> signList = new ArrayList<>();
        signList.add(signatureData);
        appendSignatures(signList);
    }

    /**
     * Appends signatures to the transaction.
     * @param signatureData List of SignatureData contains ECDSA signature data
     */
    public void appendSignatures(List<SignatureData> signatureData) {
        this.signatures.addAll(signatureData);
        this.signatures = refineSignature(this.getSignatures());
    }

    /**
     * Combines signatures to the transaction from RLP-encoded transaction strings and returns a single transaction with all signatures combined.
     * When combining the signatures into a transaction instance,
     * an error is thrown if the decoded transaction contains different value except signatures.
     * @param rlpEncoded A List of RLP-encoded transaction strings.
     * @return String
     */
    public abstract String combineSignedRawTransactions(List<String> rlpEncoded);


    /**
     * Returns a RawTransaction(RLP-encoded transaction string)
     * @return String
     */
    @JsonIgnore
    public String getRawTransaction() {
        return this.getRLPEncoding();
    }

    /**
     * Returns a hash string of transaction
     * @return String
     */
    @JsonIgnore
    public String getTransactionHash() {
        return Hash.sha3(this.getRLPEncoding());
    }

    /**
     * Returns a senderTxHash of transaction
     * @return String
     */
    @JsonIgnore
    public String getSenderTxHash() {
        return this.getTransactionHash();
    }

    /**
     * Returns an RLP-encoded transaction string for making signature.
     * @return String
     */
    @JsonIgnore
    public String getRLPEncodingForSignature() {
        byte[] txRLP = Numeric.hexStringToByteArray(getCommonRLPEncodingForSignature());

        List<RlpType> rlpTypeList = new ArrayList<>();
        rlpTypeList.add(RlpString.create(txRLP));
        rlpTypeList.add(RlpString.create(Numeric.toBigInt(this.getChainId())));
        rlpTypeList.add(RlpString.create(0));
        rlpTypeList.add(RlpString.create(0));
        byte[] encoded = RlpEncoder.encode(new RlpList(rlpTypeList));
        return Numeric.toHexString(encoded);
    }

    /**
     * Fills empty optional transaction field.(nonce, gasPrice, chainId)
     * @throws IOException
     */
    public void fillTransaction() throws IOException{
        if(klaytnCall != null) {
            if(this.nonce.equals("0x")) {
                this.nonce = klaytnCall.getTransactionCount(this.from, DefaultBlockParameterName.PENDING).send().getResult();
            }

            if(this.chainId.equals("0x")) {
                this.chainId = klaytnCall.getChainID().send().getResult();
            }
        }

        if(this.nonce.equals("0x") || this.chainId.equals("0x")) {
            throw new RuntimeException("Cannot fill transaction data.(nonce, chainId). `klaytnCall` must be set in Transaction instance to automatically fill the nonce, chainId or gasPrice. Please call the `setKlaytnCall` to set `klaytnCall` in the Transaction instance.");
        }
    }

    /**
     * Check equals txObj passed parameter and Current instance.
     * @param txObj The AbstractTransaction Object to compare
     * @param checkSig Check whether signatures field is equal.
     * @return boolean
     */
    public boolean compareTxField(AbstractTransaction txObj, boolean checkSig) {
        if(!this.getType().equals(txObj.getType())) return false;
        if(!this.getFrom().toLowerCase().equals(txObj.getFrom().toLowerCase())) return false;
        if(!Numeric.toBigInt(this.getNonce()).equals(Numeric.toBigInt(txObj.getNonce()))) return false;
        if(!Numeric.toBigInt(this.getGas()).equals(Numeric.toBigInt(txObj.getGas()))) return false;

        if(checkSig) {
            List<SignatureData> dataList = this.getSignatures();
            if(dataList.size() != txObj.getSignatures().size()) return false;

            for(int i=0; i< dataList.size(); i++) {
                if(!dataList.get(i).equals(txObj.getSignatures().get(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks that member variables that can be defined by the user are defined.
     * If there is an undefined variable, an error occurs.
     */
    public void validateOptionalValues(boolean checkChainID) {
        if(this.getNonce() == null || this.getNonce().isEmpty() || this.getNonce().equals("0x")) {
            throw new RuntimeException("nonce is undefined. Define nonce in transaction or use 'transaction.fillTransaction' to fill values.");
        }

        if(checkChainID) {
            if(this.getChainId() == null || this.getChainId().isEmpty() || this.getChainId().equals("0x")) {
                throw new RuntimeException("chainId is undefined. Define chainId in transaction or use 'transaction.fillTransaction' to fill values.");
            }
        }
    }

    /**
     * Refines the array containing signatures
     *   - Removes duplicate signatures
     *   - Removes the default empty signature("0x01", "0x", "0x")
     *   - For an empty signature array, return an array containing the default empty signature("0x01", "0x", "0x")
     * @param signatureDataList The list of {@link SignatureData}
     * @return List&lt;String&gt;
     */
    public List<SignatureData> refineSignature(List<SignatureData> signatureDataList) {
        boolean isEthereumTransaction = TransactionHelper.isEthereumTransaction(this.getType());

        List<SignatureData> refinedList = SignatureData.refineSignature(signatureDataList);

        if(isEthereumTransaction && refinedList.size() > 1) {
            throw new RuntimeException(this.getType() + " cannot have multiple signature.");
        }

        return refinedList;
    }

    /**
     * Recovers the public key strings from "signatures" field in transaction object.<p>
     * If you want to derive an address from public key, please use {@link Utils#publicKeyToAddress(String)}.
     * <pre>Example :
     * {@code
     * List<String> publicKeys = tx.recoverPublicKeys();
     * }
     * </pre>
     * @return List&lt;String&gt;
     */
    public List<String> recoverPublicKeys() {
        try {
            // If it is EthereumTyped transaction(EthereumAccessList, EthereumDynamicFee), call recoverPublicKeysWithEthereumTypedTransaction.
            if(TransactionHelper.isEthereumTypedTransaction(this.getType())) {
                return recoverPublicKeysWithEthereumTypedTransaction();
            }

            if(Utils.isEmptySig(this.getSignatures())) {
                throw new RuntimeException("Failed to recover public keys from signatures: signatures is empty.");
            }

            // For recover signature. We need to find chainId from signatures' v field.
            // The V value in Tx signatures is set by [parity value {0,1} + chainId * 2 + 35]
            // https://eips.ethereum.org/EIPS/eip-155
            if(this.getChainId().equals("0x")) {
                BigInteger chainId = this.getSignatures().get(0).getChainId();
                setChainId(chainId);
            }

            String sigHash = TransactionHasher.getHashForSignature(this);

            List<String> publicKeyList = new ArrayList<>();
            for(SignatureData signatureData : this.getSignatures()) {
                if(Numeric.toBigInt(this.getChainId()).compareTo(signatureData.getChainId()) != 0) {
                    throw new RuntimeException("Invalid Signature data : chain id is not matched.");
                }

                publicKeyList.add(Utils.recoverPublicKey(sigHash, signatureData, true));
            }
            return publicKeyList;
        } catch(SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> recoverPublicKeysWithEthereumTypedTransaction() throws SignatureException{
            if(Utils.isEmptySig(this.getSignatures())) {
                throw new RuntimeException("Failed to recover public keys from signatures: signatures is empty.");
            }

            String sigHash = TransactionHasher.getHashForSignature(this);

            List<String> publicKeyList = new ArrayList<>();
            for(SignatureData signatureData : this.getSignatures()) {
                if(Numeric.toBigInt(signatureData.getV()).compareTo(BigInteger.ZERO) != 0 && Numeric.toBigInt(signatureData.getV()).compareTo(BigInteger.ONE) != 0) {
                    throw new RuntimeException("Invalid Signature data : the v value must have 0 or 1.");
                }
                publicKeyList.add(Utils.recoverPublicKey(sigHash, signatureData, true));
            }
            return publicKeyList;
    }

    /**
     * Getter function for klaytnRPC
     * @return Klay
     */
    public Klay getKlaytnCall() {
        return klaytnCall;
    }

    /**
     * Setter function for klaytnRPC
     * @param klaytnCall Klay RPC Instance.
     */
    public void setKlaytnCall(Klay klaytnCall) {
        this.klaytnCall = klaytnCall;
    }

    /**
     * Getter function for type.
     * @return String
     */
    public String getType() {
        return type;
    }

    /**
     * Getter function for from
     * @return String
     */
    public String getFrom() {
        return from;
    }

    /**
     * Getter function for nonce
     * @return String
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Getter function for gas
     * @return String
     */
    public String getGas() {
        return gas;
    }

    /**
     * Getter function for chain id
     * @return String
     */
    @JsonIgnore
    public String getChainId() {
        return chainId;
    }

    /**
     * Getter function for signatures
     * @return String
     */
    public List<SignatureData> getSignatures() {
        return signatures;
    }

    /**
     * Setter function for type.
     * @param type The Transaction type.
     */
    public void setType(String type) {
        this.type = type;
    }

    public void setFrom(String from) {
        //"From" field in EthereumTransaction allows null
        if(TransactionHelper.isEthereumTransaction(this.getType())) {
            if(from == null || from.isEmpty() || from.equals("0x") || from.equals(Utils.DEFAULT_ZERO_ADDRESS)) from = Utils.DEFAULT_ZERO_ADDRESS;
        } else {
            if(from == null) {
                throw new IllegalArgumentException("from is missing.");
            }

            if(!Utils.isAddress(from)) {
                throw new IllegalArgumentException("Invalid address. : " + from);
            }
        }

        this.from = from;
    }

    /**
     * Setter function for gas
     * @param gas The maximum amount of gas the transaction is allowed to use.
     */
    public void setGas(String gas) {
        //Gas value must be set.
        if(gas == null || gas.isEmpty() || gas.equals("0x")) {
            throw new IllegalArgumentException("gas is missing.");
        }

        if(!Utils.isNumber(gas)) {
            throw new IllegalArgumentException("Invalid gas. : " + gas);
        }
        this.gas = gas;
    }

    /**
     * Setter function for gas
     * @param gas The maximum amount of gas the transaction is allowed to use.
     */
    public void setGas(BigInteger gas) {
        setGas(Numeric.toHexStringWithPrefix(gas));
    }

    /**
     * Setter function for nonce.
     * @param nonce A value used to uniquely identify a sender’s transaction.
     */
    public void setNonce(String nonce) {

        if(nonce == null || nonce.isEmpty() || nonce.equals("0x")) {
            nonce = "0x";
        }

        if(!nonce.equals("0x") && !Utils.isNumber(nonce)) {
            throw new IllegalArgumentException("Invalid nonce. : " + nonce);
        }
        this.nonce = nonce;
    }

    /**
     * Setter function for nonce.
     * @param nonce A value used to uniquely identify a sender’s transaction.
     */
    public void setNonce(BigInteger nonce) {
        setNonce(Numeric.toHexStringWithPrefix(nonce));
    }

    /**
     * Setter function for chain id.
     * @param chainId A network id.
     */
    public void setChainId(String chainId) {
        if(chainId == null || chainId.isEmpty() || chainId.equals("0x")) {
            chainId = "0x";
        }

        if(!chainId.equals("0x") && !Utils.isNumber(chainId)) {
            throw new IllegalArgumentException("Invalid chainId. : " + chainId);
        }

        this.chainId = chainId;
    }


    /**
     * Setter function for chain id.
     * @param chainId A network id.
     */
    public void setChainId(BigInteger chainId) {
        setChainId(Numeric.toHexStringWithPrefix(chainId));
    }

    public void setSignatures(List<SignatureData> signatures) {
        if(signatures == null || signatures.size() == 0) {
            signatures = Arrays.asList(SignatureData.getEmptySignature());
        }
        appendSignatures(signatures);
    }

    @JsonProperty("typeInt")
    public int getKeyType() {
        return TransactionType.valueOf(this.getType()).getType();
    }
}