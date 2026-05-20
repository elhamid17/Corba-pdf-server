package sn.ussein.gateway.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represente une operation PDF (merge, split, conversion...) executee
 * pour le compte d'un utilisateur authentifie ou d'un guest anonyme.
 *
 * Le fichier resultat est stocke dans GridFS ; resultGridFsId pointe dessus.
 */
@Document(collection = "jobs")
public class Job {

    @Id
    private String id;

    /** ID utilisateur si authentifie, null sinon. */
    @Indexed
    private String userId;

    /** UUID cookie anonyme si guest, null sinon. */
    @Indexed
    private String guestId;

    /** Type d'operation : "merge", "split", "compress", "ocr", "word-to-pdf"... */
    @Indexed
    private String operation;

    private JobStatus status = JobStatus.PENDING;

    /** Nom d'origine du fichier source (premier fichier si plusieurs). */
    private String inputFilename;

    private String outputFilename;

    private String outputContentType;

    /** ObjectId GridFS du fichier resultat — null si echec ou pas encore stocke. */
    private String resultGridFsId;

    private long resultSizeBytes;

    /**
     * Date a laquelle le fichier resultat doit etre purge.
     * Purge effectuee par JobCleanupTask (suppression GridFS + doc) — pas par index TTL,
     * car un index TTL supprimerait le doc Job sans nettoyer le fichier GridFS associe.
     */
    @Indexed
    private Instant expiresAt;

    private String errorMessage;

    private long durationMs;

    @CreatedDate
    @Indexed
    private Instant createdAt;

    public Job() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getInputFilename() { return inputFilename; }
    public void setInputFilename(String inputFilename) { this.inputFilename = inputFilename; }

    public String getOutputFilename() { return outputFilename; }
    public void setOutputFilename(String outputFilename) { this.outputFilename = outputFilename; }

    public String getOutputContentType() { return outputContentType; }
    public void setOutputContentType(String outputContentType) { this.outputContentType = outputContentType; }

    public String getResultGridFsId() { return resultGridFsId; }
    public void setResultGridFsId(String resultGridFsId) { this.resultGridFsId = resultGridFsId; }

    public long getResultSizeBytes() { return resultSizeBytes; }
    public void setResultSizeBytes(long resultSizeBytes) { this.resultSizeBytes = resultSizeBytes; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
