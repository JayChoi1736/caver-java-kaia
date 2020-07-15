package com.klaytn.caver.contract;

import com.klaytn.caver.utils.Utils;
import org.web3j.abi.datatypes.Type;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;

public class ContractExecuteParam {
    String from;
    String contractAddress;
    String gas;
    String gasPrice;

    List<Type> params;

    public ContractExecuteParam(String from, String contractAddress, BigInteger gas, List<Type> params) {
        this(
                from,
                contractAddress,
                Numeric.toHexStringWithPrefix(gas),
                null,
                params
        );
    }

    public ContractExecuteParam(String from, String contractAddress, String gas, List<Type> params) {
        this(
                from,
                contractAddress,
                gas,
                null,
                params
        );
    }

    public ContractExecuteParam(String from, String contractAddress, BigInteger gas, BigInteger gasPrice, List<Type> params) {
        this(
                from,
                contractAddress,
                Numeric.toHexStringWithPrefix(gas),
                Numeric.toHexStringWithPrefix(gasPrice),
                params
        );
    }

    public ContractExecuteParam(String from, String contractAddress, String gas, String gasPrice, List<Type> params) {
        setFrom(from);
        setContractAddress(contractAddress);
        setGas(gas);
        setGasPrice(gasPrice);
        setParams(params);
    }

    public String getFrom() {
        return from;
    }

    public String getGas() {
        return gas;
    }

    public String getGasPrice() {
        return gasPrice;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public List<Type> getParams() {
        return params;
    }

    public void setContractAddress(String contractAddress) {
        if(contractAddress == null) {
            throw new IllegalArgumentException("to is missing.");
        }

        if(!Utils.isAddress(contractAddress)) {
            throw new IllegalArgumentException("Invalid address. : " + contractAddress);
        }

        this.contractAddress = contractAddress;
    }

    public void setFrom(String from) {
        if(from == null) {
            throw new IllegalArgumentException("from is missing.");
        }

        if(!Utils.isAddress(from)) {
            throw new IllegalArgumentException("Invalid address. : " + from);
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
            throw new IllegalArgumentException("Invalid gas. : "  + gas);
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
     * Setter function for gas price.
     * @param gasPrice A unit price of gas in peb the sender will pay for a transaction fee.
     */
    public void setGasPrice(String gasPrice) {
        if(gasPrice == null || gasPrice.isEmpty() || gasPrice.equals("0x")) {
            gasPrice = "0x";
        }

        if(!gasPrice.equals("0x") && !Utils.isNumber(gasPrice)) {
            throw new IllegalArgumentException("Invalid gasPrice. : " + gasPrice);
        }

        this.gasPrice = gasPrice;
    }

    /**
     * Setter function for gas price.
     * @param gasPrice A unit price of gas in peb the sender will pay for a transaction fee.
     */
    public void setGasPrice(BigInteger gasPrice) {
        setGasPrice(Numeric.toHexStringWithPrefix(gasPrice));
    }

    public void setParams(List<Type> params) {
        this.params = params;
    }
}
