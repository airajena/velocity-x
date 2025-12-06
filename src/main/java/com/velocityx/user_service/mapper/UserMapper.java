package com.velocityx.user_service.mapper;

import com.velocityx.user_service.dto.response.UserResponse;
import com.velocityx.user_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for User entity to DTO conversions.
 * Provides type-safe, compile-time validated mappings.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "accountStatus", expression = "java(user.getAccountStatus().name())")
    UserResponse toUserResponse(User user);
}
