package com.ebanking.user.dto;

import com.ebanking.user.entity.Customer.KycStatus;
import lombok.Data;

public class CustomerDTOs {

    @Data
    public static class UpdateProfileRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String address;
    }

    @Data
    public static class UpdateKYCRequest {
        private KycStatus status;
    }
}
