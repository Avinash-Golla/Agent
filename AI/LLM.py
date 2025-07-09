from flask import Flask, request, jsonify
import subprocess

app = Flask(__name__)

@app.route('/query', methods=['POST'])
def query():
    data = request.get_json()
    prompt = data.get("prompt")

    try:
        result = subprocess.run(
            ['ollama', 'run', 'gemma:2b', prompt],
            capture_output=True,
            text=True,
            encoding='utf-8'  # Fix for UnicodeDecodeError
        )

        if result.returncode != 0:
            return jsonify({"error": "Failed to run model", "details": result.stderr}), 500

        return jsonify({"response": result.stdout.strip()})

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(port=5005)
