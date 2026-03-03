package interfaces.mentorat;

import models.mentorat.MentorshipSession;
import java.util.List;

public interface IMentorshipSession extends IService<MentorshipSession> {
    List<MentorshipSession> getByRequestId(int requestId);
}

