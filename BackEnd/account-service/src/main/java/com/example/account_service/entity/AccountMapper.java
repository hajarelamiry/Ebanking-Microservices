package com.example.account_service.entity;
import com.example.account_service.dto.AccountDto;
import com.example.account_service.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    // Entity -> DTO
    public AccountDto toDto(Account account) {
        if (account == null) return null;

        return AccountDto.builder()
                .externalReference(account.getExternalReference())
                .balance(account.getBalance())
                .devise(account.getDevise())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    // DTO -> Entity (rarement utilisé)
    // L'utilisateurId vient du JWT (service layer)
    public Account toEntity(AccountDto dto, String utilisateurId) {
        if (dto == null) return null;

        return Account.builder()
                .externalReference(dto.getExternalReference())
                .balance(dto.getBalance())
                .devise(dto.getDevise())
                .status(dto.getStatus())
                .utilisateurId(utilisateurId)
                .build();
    }
}
