package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Account;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferStatus;
import com.techelevator.tenmo.model.TransferType;
import com.techelevator.tenmo.pojos.UserPojo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcAccountDao implements AccountDao {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Account findAccountByUserId(int userId) {
        String sql = "SELECT account_id, user_id, balance FROM account WHERE user_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);
        if (results.next()) {
            return mapRowToAccount(results);
        }
        return null;
    }

    @Override
    public Account findAccountByAccountId(int accountId) {
        String sql = "SELECT account_id, user_id, balance FROM account WHERE account_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, accountId);
        if (results.next()) {
            return mapRowToAccount(results);
        }
        return null;
    }

    @Override
    public void updateBalance(int userId, BigDecimal newSenderBalance) {
        String sql = "UPDATE account SET balance = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, newSenderBalance, userId);
    }

    @Override
    public List<Transfer> findTransfersByUserId(int userId) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, from_user_id, to_user_id, amount " +
                "FROM transfer " +
                "WHERE from_user_id = ? OR to_user_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId, userId);
        while (results.next()) {
            Transfer transfer = mapRowToTransfer(results);
            transfers.add(transfer);
        }
        return transfers;
    }

    @Override
    public List<Transfer> findPendingTransfersByUserId(int userId) {
        List<Transfer> pendingTransfers = new ArrayList<>();
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, from_user_id, to_user_id, amount " +
                "FROM transfer " +
                "WHERE (from_user_id = ? OR to_user_id = ?) AND transfer_status_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId, userId, TransferStatus.PENDING);
        while (results.next()) {
            Transfer transfer = mapRowToTransfer(results);
            pendingTransfers.add(transfer);
        }
        return pendingTransfers;
    }

    @Override
    public BigDecimal findBalanceByUserId(int userId) {
        String sql = "SELECT balance FROM account WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
    }

    @Override
    public BigDecimal subtractBalance(int userId, BigDecimal amountToSubtract) {
        Account account = findAccountByUserId(userId);
        BigDecimal newBalance = account.getBalance().subtract(amountToSubtract);
        updateBalance(userId, newBalance);
        return newBalance;
    }

    @Override
    public BigDecimal addBalance(int userId, BigDecimal amountToAdd) {
        Account account = findAccountByUserId(userId);
        BigDecimal newBalance = account.getBalance().add(amountToAdd);
        updateBalance(userId, newBalance);
        return newBalance;
    }

    @Override
    public Transfer saveTransfer(Transfer transfer) {

        String sql = "INSERT INTO transfer (transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
                "VALUES (1, 1, ?, ?, ?) returning transfer_id";

        int fromUser = findAccountByUserId(transfer.getFromUser()).getAccountId();
        int toUser = findAccountByUserId(transfer.getToUser()).getAccountId();

        Integer newTransferId = jdbcTemplate.queryForObject(sql, Integer.class, fromUser, toUser, transfer.getAmount());
        transfer.setTransferType(TransferType.SEND);
        transfer.setTransferStatus(TransferStatus.APPROVED);
        transfer.setTransferId(newTransferId);
        return transfer;
    }



    @Override
    public void updateTransferStatus(Transfer transfer) {
        String sql = "UPDATE transfer SET transfer_status_id = ? WHERE transfer_id = ?";
        jdbcTemplate.update(sql, transfer.getTransferStatus(), transfer.getTransferId());
    }

    @Override
    public List<Transfer> getTransfersByUserId(int userId) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, account_from, account_to, amount " +
                     "FROM transfer " +
                     "WHERE account_from = (SELECT account_id FROM account WHERE user_id = ?) " +
                     "OR account_to = (SELECT account_id FROM account WHERE user_id = ?)";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId, userId);
        while (results.next()) {
            Transfer transfer = mapRowToTransfer(results);
            transfers.add(transfer);
        }
        return transfers;
    }

    @Override
    public List<Transfer> getTransfersByAccountId(int accountId) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, account_from, account_to, amount " +
                "FROM transfer " +
                "WHERE account_from = ? OR account_to = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, accountId, accountId);
        while (results.next()) {
            Transfer transfer = mapRowToTransfer(results);
            transfers.add(transfer);
        }
        return transfers;
    }

    @Override
    public Transfer findTransferByTransferId(int transferId) {
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, from_user_id, to_user_id, amount " +
                "FROM transfer " +
                "WHERE transfer_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, transferId);
        if (results.next()) {
            return mapRowToTransfer(results);
        }
        return null;
    }

    @Override
    public Transfer getTransferDetails(int transferId) {
        String sql = "SELECT transfer_id, transfer_type_id, transfer_status_id, account_from, account_to, amount " +
                "FROM transfer " +
                "WHERE transfer_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, transferId);
        if (results.next()) {
            return mapRowToTransfer(results);
        }
        return null;
    }

    @Override
    public int getUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM tenmo_user WHERE username = ?";
        try {
            Integer userId = jdbcTemplate.queryForObject(sql, Integer.class, username);
            return userId != null ? userId : 0; // Return 0 if userId is null
        } catch (EmptyResultDataAccessException e) {
            return 0; // Return 0 if the username is not found
        }
    }

    private UserPojo findUserByAccountId(int accountId) {
        String sql = "SELECT tenmo_user.user_id, username FROM tenmo_user JOIN account ON tenmo_user.user_id = account.user_id WHERE account_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, accountId);
        if (results.next()) {
            return mapRowToUser(results);
        }
        return null;
    }

    private Transfer mapRowToTransfer(SqlRowSet results) {
        Transfer transfer = new Transfer();
        transfer.setTransferId(results.getInt("transfer_id"));
        transfer.setTransferType(results.getInt("transfer_type_id"));
        transfer.setTransferStatus(results.getInt("transfer_status_id"));
        transfer.setFromUser(
                findUserByAccountId(results.getInt("from_user_id")).getUserId());
        transfer.setToUser(
                findUserByAccountId(results.getInt("to_user_id")).getUserId());
        transfer.setAmount(results.getBigDecimal("amount"));
        return transfer;
    }

    private Account mapRowToAccount(SqlRowSet results) {
        Account account = new Account();
        account.setAccountId(results.getInt("account_id"));
        account.setUser(findUserByAccountId(results.getInt("account_id")));
        account.setBalance(results.getBigDecimal("balance"));
        return account;
    }
    private UserPojo mapRowToUser(SqlRowSet results) {
        UserPojo user = new UserPojo();
        user.setUserId(results.getInt("user_id"));
        user.setUsername(results.getString("username"));
        return user;
    }
}
