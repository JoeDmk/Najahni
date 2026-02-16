package tn.esprit.interfaces;

import tn.esprit.models.MentorAvailability;
import java.util.List;

public interface IMentorAvailability extends IService<MentorAvailability> {
    List<MentorAvailability> getByMentorId(int mentorId);
}
