package by.psu.service.dto.mappers.user;

import by.psu.model.postgres.UserProfile;
import by.psu.service.dto.UserProfileDTO;
import by.psu.service.dto.mappers.MapperConfiguration;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfiguration.class)
public interface UserProfileMapper {

    public UserProfileDTO map(UserProfile userProfile);

    public UserProfile map(UserProfileDTO userProfileDTO);

}
