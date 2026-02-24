import { useState } from 'react';
import { useAuthStore } from '../store/authStore';
import { memberService } from '../services/memberService';
import { useNavigate } from 'react-router-dom';
import { TbEdit, TbX } from 'react-icons/tb';

const ProfilePage = () => {
  const { user, setUser } = useAuthStore();
  const navigate = useNavigate();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const [form, setForm] = useState({
    username: user?.username || '',
    email: user?.email || '',
    careerYears: user?.careerYears || 0,
  });

  if (!user) return null;

  const handleSave = async () => {
    setIsLoading(true);
    try {
      await memberService.updateUsername({ username: form.username });

      await memberService.updateProfile({
        careerYears: form.careerYears,
      });

      if (form.email !== user.email) {
        await memberService.updateEmail({ email: form.email });
      }

      const updated = await memberService.getMe();
      setUser(updated);

      setSuccessMessage('정보가 수정되었습니다.');

      setTimeout(() => {
        setSuccessMessage('');
        setIsModalOpen(false);
      }, 1500);

    } catch (err) {
      setSuccessMessage('수정 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F6F8] pt-24 pb-20 px-6">
      <div className="max-w-5xl mx-auto space-y-10">

        {/* 프로필 카드 */}
        <div className="relative rounded-3xl p-10 text-white overflow-hidden
          bg-gradient-to-r from-[#36C9C6] to-[#7FB8A4] shadow-xl">

          <button
            onClick={() => setIsModalOpen(true)}
            className="absolute top-6 right-6 bg-white/20 hover:bg-white/30 px-4 py-2 rounded-lg text-sm flex items-center gap-2 transition"
          >
            <TbEdit size={16} />
            정보 수정
          </button>

          <div className="flex items-center gap-10">
            <div className="w-28 h-28 rounded-full bg-white/30 flex items-center justify-center text-4xl font-bold">
              {user.username?.charAt(0).toUpperCase()}
            </div>

            <div>
              <h2 className="text-3xl font-bold">{user.username}님</h2>
              <p className="text-white/80">{user.email}</p>
              <p className="text-white/70 text-sm mt-2">
                {user.careerYears === 0 ? '신입' : `${user.careerYears}년차`}
              </p>
            </div>
          </div>
        </div>

        {/* 공고 모아보기 */}
        <div
          onClick={() => navigate('/archive')}
          className="bg-white p-8 rounded-2xl border border-gray-100 shadow-sm cursor-pointer hover:border-[#4CDFD5]/40 transition-all"
        >
          <h3 className="text-lg font-bold">공고 모아보기</h3>
          <p className="text-sm text-gray-400 mt-1">
            내가 등록하거나 저장한 공고를 확인하세요.
          </p>
        </div>

      </div>

      {/* 수정 모달 */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">

          <div className="bg-white w-full max-w-lg rounded-2xl p-8 relative animate-fadeIn">

            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-700"
            >
              <TbX size={20} />
            </button>

            <h2 className="text-xl font-bold mb-6">정보 수정</h2>

            {successMessage && (
              <div className="mb-5 flex items-center gap-3 px-4 py-3 rounded-xl
                bg-[#4CDFD5]/10 border border-[#4CDFD5]/30
                text-[#0f172a] text-sm font-semibold animate-fadeIn">

                <div className="w-6 h-6 rounded-full bg-[#4CDFD5] flex items-center justify-center text-white text-xs">
                  ✓
                </div>

                {successMessage}
              </div>
            )}

            <div className="space-y-4">

              <Input
                label="닉네임"
                value={form.username}
                onChange={(v: string) =>
                  setForm({ ...form, username: v })
                }
              />

              <Input
                label="이메일"
                value={form.email}
                onChange={(v: string) =>
                  setForm({ ...form, email: v })
                }
              />

              <Input
                label="경력(년)"
                type="number"
                value={form.careerYears}
                onChange={(v: string) =>
                  setForm({
                    ...form,
                    careerYears: parseInt(v) || 0,
                  })
                }
              />

              <button
                onClick={handleSave}
                disabled={isLoading}
                className="w-full py-3 bg-[#4CDFD5] hover:bg-[#3CCFC5]
                  text-white rounded-xl font-semibold mt-4
                  transition disabled:opacity-50"
              >
                {isLoading ? '저장 중...' : '저장하기'}
              </button>

            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const Input = ({
  label,
  value,
  onChange,
  type = 'text',
}: any) => (
  <div>
    <label className="text-sm text-gray-500">{label}</label>
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full mt-1 px-4 py-3 rounded-xl border border-gray-200
        focus:border-[#4CDFD5] focus:ring-4 focus:ring-[#4CDFD5]/20 outline-none transition"
    />
  </div>
);

export default ProfilePage;
