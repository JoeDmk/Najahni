package tn.esprit.interfaces;

import tn.esprit.models.MentorshipSession;
import java.util.List;

public interface IMentorshipSession extends IService<MentorshipSession> {
    List<MentorshipSession> getByRequestId(int requestId);
}
