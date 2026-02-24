import { useEffect, useState } from 'react';
import { useAuthStore } from '../store/authStore';
import { memberService } from '../services/memberService';
import { toast } from 'react-toastify';
import { TbUser, TbBriefcase, TbCalendar, TbFileDescription, TbEdit, TbCheck, TbX, TbAlertTriangle } from 'react-icons/tb';

const ProfilePage = () => {
  const { user, setUser } = useAuthStore();
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState({
    username: '',
    careerYears: 0,
    summary: '',
    currentPositionId: undefined as number | undefined
  });

  useEffect(() => {
    if (user) {
      setFormData({
        username: user.username || '',
        careerYears: user.careerYears || 0,
        summary: user.summary || '',
        currentPositionId: user.currentPositionId
      });
    }
  }, [user]);

  const handleSave = async () => {
    setIsLoading(true);
    try {
      // 1. Update Username if changed
      if (formData.username !== user?.username) {
        await memberService.updateUsername({ username: formData.username });
      }

      // 2. Update Profile
      await memberService.updateProfile({
        careerYears: formData.careerYears,
        summary: formData.summary,
        currentPositionId: formData.currentPositionId
      });

      // 3. Refresh Store
      const updatedUser = await memberService.getMe();
      setUser(updatedUser);
      
      setIsEditing(false);
    } catch (error) {
      console.error('Failed to update profile', error);
      toast.error('프로필 저장 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleWithdraw = async () => {
    if (window.confirm('정말로 탈퇴하시겠습니까? 모든 데이터가 삭제되며 복구할 수 없습니다.')) {
      try {
        await memberService.withdraw();
        window.location.href = '/';
      } catch (error) {
        toast.error('탈퇴 처리 중 오류가 발생했습니다.');
      }
    }
  };

  if (!user) return null;

  return (
    <div className="min-h-screen bg-[#F8F9FA] pt-24 pb-20 px-6 font-primary text-gray-900">
      <div className="max-w-4xl mx-auto">
        
        {/* Profile Header */}
        <div className="flex flex-col md:flex-row items-center gap-8 mb-12">
          <div className="w-32 h-32 rounded-[2.5rem] bg-gradient-to-tr from-[#276db8] to-[#89cbb6] flex items-center justify-center text-white text-5xl font-black shadow-2xl shadow-[#89cbb6]/20 rotate-3 hover:rotate-0 transition-transform duration-500">
            {user.username?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div className="text-center md:text-left">
            <h1 className="text-[10px] font-black text-[#89cbb6] uppercase tracking-[0.4em] mb-2 italic">Official ID Card</h1>
            <h2 className="text-4xl font-black tracking-tight mb-2 italic">{user.name}</h2>
            <div className="flex items-center justify-center md:justify-start gap-3">
              <span className="text-sm font-bold text-gray-400">{user.email}</span>
              <div className="w-1 h-1 rounded-full bg-gray-200" />
              <span className="text-xs font-black text-[#276db8] uppercase tracking-widest">Active Member</span>
            </div>
          </div>
        </div>

        {/* Info Grid (Logbook Style) */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
          
          {/* Card 1: Username */}
          <div className="bg-white rounded-[2rem] border border-gray-100 p-8 shadow-sm hover:shadow-lg transition-all group">
            <div className="flex justify-between items-start mb-6">
              <div className="w-12 h-12 bg-gray-50 rounded-2xl flex items-center justify-center text-gray-400 group-hover:text-[#276db8] transition-colors">
                <TbUser size={24} />
              </div>
              <button onClick={() => setIsEditing(true)} className="text-gray-300 hover:text-[#276db8] transition-colors">
                <TbEdit size={20} />
              </button>
            </div>
            <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Display Name</h3>
            {isEditing ? (
              <input 
                type="text"
                className="w-full text-lg font-black text-[#276db8] bg-transparent border-b-2 border-[#276db8]/20 focus:border-[#276db8] outline-none py-1"
                value={formData.username}
                onChange={e => setFormData({...formData, username: e.target.value})}
              />
            ) : (
              <p className="text-xl font-black">{user.username}</p>
            )}
          </div>

          {/* Card 2: Career */}
          <div className="bg-white rounded-[2rem] border border-gray-100 p-8 shadow-sm hover:shadow-lg transition-all group">
            <div className="flex justify-between items-start mb-6">
              <div className="w-12 h-12 bg-gray-50 rounded-2xl flex items-center justify-center text-gray-400 group-hover:text-[#89cbb6] transition-colors">
                <TbCalendar size={24} />
              </div>
            </div>
            <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Experience</h3>
            {isEditing ? (
              <div className="flex items-center gap-2">
                <input 
                  type="number"
                  className="w-20 text-lg font-black text-[#89cbb6] bg-transparent border-b-2 border-[#89cbb6]/20 focus:border-[#89cbb6] outline-none py-1"
                  value={formData.careerYears}
                  onChange={e => setFormData({...formData, careerYears: parseInt(e.target.value) || 0})}
                />
                <span className="font-bold text-gray-400">Years</span>
              </div>
            ) : (
              <p className="text-xl font-black">{user.careerYears || 0} Years</p>
            )}
          </div>

          {/* Card 3: Position (Mocked for now as we don't have position list) */}
          <div className="bg-white rounded-[2rem] border border-gray-100 p-8 shadow-sm hover:shadow-lg transition-all group">
            <div className="flex justify-between items-start mb-6">
              <div className="w-12 h-12 bg-gray-50 rounded-2xl flex items-center justify-center text-gray-400 group-hover:text-[#276db8] transition-colors">
                <TbBriefcase size={24} />
              </div>
            </div>
            <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Current Position</h3>
            <p className="text-xl font-black text-gray-300 italic">{user.currentPositionId ? 'ID: ' + user.currentPositionId : 'Not Specified'}</p>
          </div>
        </div>

        {/* Professional Summary */}
        <div className="bg-white rounded-[2.5rem] border border-gray-100 p-8 md:p-12 shadow-sm mb-12">
          <div className="flex items-center justify-between mb-8">
            <div className="flex items-center gap-4">
              <div className="w-10 h-10 rounded-xl bg-gray-900 flex items-center justify-center text-white shadow-lg">
                <TbFileDescription size={20} />
              </div>
              <h3 className="text-xl font-black tracking-tight italic">Professional Logbook Summary</h3>
            </div>
          </div>
          
          {isEditing ? (
            <textarea 
              className="w-full h-40 p-6 rounded-2xl bg-gray-50 border border-gray-100 focus:bg-white focus:border-[#276db8] focus:ring-4 focus:ring-[#276db8]/5 outline-none transition-all resize-none font-medium leading-relaxed"
              placeholder="자신을 한 줄로 표현해 보세요."
              value={formData.summary}
              onChange={e => setFormData({...formData, summary: e.target.value})}
            />
          ) : (
            <div className="min-h-[100px] p-6 rounded-2xl bg-gray-50/50 border border-dashed border-gray-200">
              <p className="text-gray-600 font-medium leading-relaxed whitespace-pre-line">
                {user.summary || '경력 요약이 등록되지 않았습니다. 자신만의 전문성을 기록해 보세요.'}
              </p>
            </div>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-3">
            {isEditing ? (
              <>
                <button 
                  onClick={handleSave}
                  disabled={isLoading}
                  className="flex items-center gap-2 px-8 py-4 bg-gray-900 text-white font-black rounded-2xl hover:bg-black hover:scale-105 active:scale-95 transition-all shadow-xl shadow-black/10 disabled:opacity-50"
                >
                  <TbCheck size={20} />
                  Save Changes
                </button>
                <button 
                  onClick={() => setIsEditing(false)}
                  className="flex items-center gap-2 px-8 py-4 bg-white border-2 border-gray-100 text-gray-400 font-black rounded-2xl hover:bg-gray-50 active:scale-95 transition-all"
                >
                  <TbX size={20} />
                  Cancel
                </button>
              </>
            ) : (
              <button 
                onClick={() => setIsEditing(true)}
                className="flex items-center gap-2 px-8 py-4 bg-white border-2 border-[#276db8] text-[#276db8] font-black rounded-2xl hover:bg-[#276db8]/5 hover:scale-105 active:scale-95 transition-all shadow-lg shadow-[#276db8]/10"
              >
                <TbEdit size={20} />
                Edit My Logbook
              </button>
            )}
          </div>

          <button 
            onClick={handleWithdraw}
            className="flex items-center gap-2 text-sm font-bold text-gray-300 hover:text-red-400 transition-colors uppercase tracking-widest italic"
          >
            <TbAlertTriangle size={18} />
            Leave HireLog
          </button>
        </div>

      </div>
    </div>
  );
};

export default ProfilePage;
