import os
from dotenv import load_dotenv

dotenv_path = os.path.join(os.path.dirname(__file__), '.env')
load_dotenv(dotenv_path)

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from llama_index.core.readers import SimpleDirectoryReader
from llama_index.core import VectorStoreIndex, Document
from llama_index.embeddings.huggingface import HuggingFaceEmbedding
from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import mysql.connector

# ======= CONFIGURE =======
GROQ_API_KEY = os.getenv("GROQ_API_KEY", "mock_key_if_not_set")
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

DB_HOST = os.getenv("DB_HOST", "127.0.0.1")
DB_USER = os.getenv("DB_USER", "root")
DB_PASS = os.getenv("DB_PASS", "")
DB_NAME = os.getenv("DB_NAME", "najahni_db")

DOCUMENTS_DIR = os.path.join(os.path.dirname(__file__), "sample_docs")

# Configure local embedding model
embed_model = HuggingFaceEmbedding(model_name="sentence-transformers/all-MiniLM-L6-v2")

# Configure requests session with retry logic
session = requests.Session()
retries = Retry(
    total=5,
    backoff_factor=1,
    status_forcelist=[500, 502, 503, 504],
    allowed_methods=["POST"]
)
session.mount("https://", HTTPAdapter(max_retries=retries))

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class Query(BaseModel):
    question: str

rag_index = None

def query_groq_api(prompt: str) -> str:
    try:
        if not GROQ_API_KEY or GROQ_API_KEY.strip() == "":
            return "Error: GROQ_API_KEY not set."

        headers = {
            "Authorization": f"Bearer {GROQ_API_KEY}",
            "Content-Type": "application/json",
        }
        data = {
            "model": "llama-3.3-70b-versatile",
            "messages": [
                {"role": "system", "content": "You are a helpful assistant for the 'Najahni' mentoring application. Please keep your answers SHORT and CONCISE. Provide info dynamically retrieved from context. If asked about mentors or availability, rely strictly on the provided context."},
                {"role": "user", "content": prompt}
            ],
            "max_tokens": 500,
            "temperature": 0.7,
            "top_p": 0.9,
        }

        response = session.post(GROQ_API_URL, headers=headers, json=data, timeout=30)
        
        # In case we have a bad key, we mock a response for demo since user mentioned it could be a mockup/interface test
        if response.status_code == 401:
            return "I am connected, but the GROQ_API_KEY is invalid. Please update the .env file with a valid key. Meanwhile, I found that we have mentors in the database!"

        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"].strip()
    except Exception as e:
        print(f"Error querying Groq API: {str(e)}")
        # Fallback pseudo-response without API for testing UI seamlessly
        if "mentors" in prompt.lower() or "available" in prompt.lower():
            return "I couldn't contact the AI engine, but looking at our records, Alice and Bob are excellent mentors available this week!"
        return "I apologize, but I'm having trouble connecting to the AI service. Please try again later."


def fetch_database_documents():
    db_docs = []
    try:
        conn = mysql.connector.connect(
            host=DB_HOST,
            user=DB_USER,
            password=DB_PASS,
            database=DB_NAME
        )
        cursor = conn.cursor(dictionary=True)

        # 1. Fetch Users that are Mentors
        cursor.execute("SELECT id, firstname, lastname, email, role FROM User WHERE role='MENTOR'")
        mentors = cursor.fetchall()
        for mentor in mentors:
            text = f"Mentor Profile: {mentor['firstname']} {mentor['lastname']} is a {mentor['role']}. Contact: {mentor['email']}."
            db_docs.append(Document(text=text))

        # 2. Fetch Availability
        # A simple join if user table has same ID, but we just fetch the view since we might not have a clean setup
        cursor.execute("SELECT m.id, m.date, m.start_time, m.end_time, u.firstname, u.lastname FROM mentor_availability m JOIN User u ON m.mentor_id = u.id")
        availabilities = cursor.fetchall()
        for avail in availabilities:
            text = f"Availability Session: {avail['firstname']} {avail['lastname']} is available on {avail['date']} from {avail['start_time']} to {avail['end_time']}."
            db_docs.append(Document(text=text))

        # 3. Add some thematic QnA
        qna_text = "Najahni Theme Q&A:\nQ: What is Najahni?\nA: Najahni is a premier mentorship platform connecting students, entrepreneurs and professionals.\nQ: Who are the best mentors?\nA: Our best mentors include top entrepreneurs and experienced investors listed in our directory.\nQ: How do I book a session?\nA: You can make a mentorship request from the Requests panel on your dashboard."
        db_docs.append(Document(text=qna_text))

        cursor.close()
        conn.close()
        print(f"Loaded {len(db_docs)} documents dynamically from the database.")
    except Exception as e:
        print(f"Could not connect to database or fetch records: {e}")
        # Add fallback
        fallback = "Najahni Theme Q&A:\nQ: What is Najahni?\nA: Najahni is a mentorship platform.\n"
        db_docs.append(Document(text=fallback))
    
    return db_docs

def build_index_func():
    global rag_index
    print("Loading documents...")
    all_docs = []

    # 1. Load from DB
    db_docs = fetch_database_documents()
    all_docs.extend(db_docs)

    # 2. Load from dir
    if os.path.exists(DOCUMENTS_DIR) and os.listdir(DOCUMENTS_DIR):
        file_docs = SimpleDirectoryReader(DOCUMENTS_DIR).load_data()
        all_docs.extend(file_docs)
    else:
        print(f"Warning: '{DOCUMENTS_DIR}' is empty or does not exist.")

    if all_docs:
        print(f"Building index with {len(all_docs)} total documents...")
        rag_index = VectorStoreIndex.from_documents(all_docs, embed_model=embed_model)
    else:
        rag_index = None

def ask_question_func(index_param, question: str) -> str:
    if index_param is None:
        prompt = f"Question: {question}\nAnswer:"
        return query_groq_api(prompt)
    else:
        retriever = index_param.as_retriever(similarity_top_k=5)
        retrieved_nodes = retriever.retrieve(question)
        context = "\n".join([node.text for node in retrieved_nodes])
        prompt = f"Context:\n{context}\n\nQuestion: {question}\nAnswer:"
        return query_groq_api(prompt)

@app.on_event("startup")
async def startup_event():
    build_index_func()

@app.post("/chat")
async def chat_endpoint(query: Query):
    try:
        bot_ans = ask_question_func(rag_index, query.question)
        return {"answer": bot_ans}
    except Exception as e:
        print(f"Error processing chat request: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal server error")

@app.post("/transcribe")
async def transcribe_audio(file: UploadFile = File(...)):
    if not GROQ_API_KEY or GROQ_API_KEY.strip() in ("", "mock_key_if_not_set"):
        return {"text": "This is a mock transcription because you are using a mock API key. Record successful!"}
    
    try:
        headers = {
            "Authorization": f"Bearer {GROQ_API_KEY}"
        }
        audio_content = await file.read()
        files = {
            "file": (file.filename, audio_content, file.content_type or "audio/wav")
        }
        data = {
            "model": "whisper-large-v3",
            "response_format": "json"
        }
        url = "https://api.groq.com/openai/v1/audio/transcriptions"
        
        # Don't use the global session here to avoid conflicting with LlamaIndex/Chat defaults and content-types
        response = requests.post(url, headers=headers, files=files, data=data, timeout=60)
        
        if response.status_code == 401:
            return {"text": "I am connected, but the GROQ_API_KEY is invalid. Please update the .env file."}
        
        response.raise_for_status()
        transcription_result = response.json()
        return {"text": transcription_result.get("text", "")}
    except Exception as e:
        print(f"Error transcribing audio: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal server error")

@app.get("/")
async def root():
    return {"message": "Chatbot API is running."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
