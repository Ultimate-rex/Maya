import json
import shutil
import time
from pathlib import Path
from typing import Any, List, Dict
from core.config import MEMORY_DIR


class MemoryManager:
    """Handles Maya's JSON-based memory. One file per concern, never one
    giant blob, so reads/writes stay fast and diffable."""

    def __init__(self, filename: str):
        self.path: Path = MEMORY_DIR / filename
        if not self.path.exists():
            self.path.write_text("[]" if filename != "preferences.json" else "{}")

    def read(self) -> Any:
        return json.loads(self.path.read_text())

    def write(self, data: Any) -> None:
        self.path.write_text(json.dumps(data, indent=2, ensure_ascii=False))

    def append_entry(self, entry: Dict) -> None:
        data = self.read()
        if not isinstance(data, list):
            raise TypeError(f"{self.path.name} is not a list; use write() for objects")
        entry["timestamp"] = time.time()
        data.append(entry)
        self.write(data)

    def search(self, predicate) -> List[Dict]:
        data = self.read()
        if not isinstance(data, list):
            return []
        return [item for item in data if predicate(item)]

    def backup(self) -> Path:
        backup_path = self.path.with_suffix(f".{int(time.time())}.bak.json")
        shutil.copy(self.path, backup_path)
        return backup_path

    def restore(self, backup_path: Path) -> None:
        shutil.copy(backup_path, self.path)


conversations = MemoryManager("conversations.json")
facts = MemoryManager("facts.json")
preferences = MemoryManager("preferences.json")
