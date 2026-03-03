package interfaces.mentorat;

import models.mentorat.MentorAvailability;
import java.util.List;

public interface IMentorAvailability extends IService<MentorAvailability> {
    List<MentorAvailability> getByMentorId(int mentorId);
}

