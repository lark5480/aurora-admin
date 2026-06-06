package com.aurora.admin.mapper;

import com.aurora.admin.entity.UserAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserAddressMapper {

    @Select("SELECT * FROM t_user_address WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY is_default DESC, create_time DESC")
    List<UserAddress> findByUserId(Long userId);

    @Select("SELECT * FROM t_user_address WHERE id = #{id} AND is_deleted = 0")
    UserAddress findById(Long id);

    @Insert("INSERT INTO t_user_address(user_id, receiver_name, receiver_phone, province, city, district, detail, is_default) " +
            "VALUES(#{userId}, #{receiverName}, #{receiverPhone}, #{province}, #{city}, #{district}, #{detail}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAddress address);

    @Update("UPDATE t_user_address SET receiver_name = #{receiverName}, receiver_phone = #{receiverPhone}, " +
            "province = #{province}, city = #{city}, district = #{district}, detail = #{detail}, " +
            "is_default = #{isDefault} WHERE id = #{id} AND is_deleted = 0")
    int update(UserAddress address);

    @Update("UPDATE t_user_address SET is_deleted = 1 WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE t_user_address SET is_default = 0 WHERE user_id = #{userId} AND is_deleted = 0")
    int clearDefault(Long userId);

    @Update("UPDATE t_user_address SET is_default = 1 WHERE id = #{id} AND is_deleted = 0")
    int setDefault(Long id);
}
