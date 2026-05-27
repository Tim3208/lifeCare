package userinfo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface UserProfileDao {
    @Insert
    void insert(UserProfile profile);

    @Update
    void update(UserProfile profile);
}